package be.lghs.accounting.services;

import be.lghs.accounting.configuration.AccountingConfiguration;
import be.lghs.accounting.model.tables.records.AccountsRecord;
import be.lghs.accounting.model.tables.records.CodasRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.AccountRepository;
import be.lghs.accounting.repositories.CodaRepository;
import be.lghs.accounting.repositories.MovementRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodasService {

   private static BigDecimal parseCodaAmount(long valueFull) {
       return BigDecimal.valueOf(valueFull, 3);
   }

   private final CodaRepository codaRepository;
   private final AccountRepository accountRepository;
   private final MovementRepository movementRepository;
   private final AccountingConfiguration config;

   private JsonNode parseCoda(byte[] content) throws IOException {
       Process process = new ProcessBuilder(config.getCodaRs(), "-e", "windows-1250", "json", "/dev/stdin")
               .start();

       try (OutputStream outputStream = process.getOutputStream()) {
           outputStream.write(content);
       }

       JsonNode tree = new ObjectMapper().readTree(process.getInputStream());

       try {
           process.waitFor(100, TimeUnit.MILLISECONDS);
       } catch (InterruptedException e) {
           if (process.isAlive()) {
               throw new RuntimeException(e);
           } else {
               // doesn't matter if we got interrupted, let's finish handling uploads first
           }
       }
       if (process.exitValue() != 0) {
           throw new RuntimeException(StreamUtils.copyToString(process.getErrorStream(), StandardCharsets.UTF_8));
       }

       return tree;
   }

   @Transactional
   public void handleCodaUpload(UUID accountId, String filename, InputStream content) throws IOException {
       AccountsRecord account = accountRepository.findOne(accountId)
               .orElseThrow(() -> new EmptyResultDataAccessException(1));

       byte[] bytes = StreamUtils.copyToByteArray(content);

       BigDecimal total = account.getCurrentBalance();

       JsonNode root = parseCoda(bytes);

       Iterator<JsonNode> elements = root.elements();
       while (elements.hasNext()) {
           JsonNode element = elements.next();

           CodasRecord coda = new CodasRecord();
           coda.setFilename(filename);
           coda.setContent(bytes);
           coda.setAccountId(accountId);
           coda.setSequenceNumber(Integer.valueOf(element.get("old_balance").get("coda_sequence").asText()));

           UUID codaId = codaRepository.createOne(coda);

           total = handleCoda(codaId, filename, total, account, element);
       }
   }

   private BigDecimal handleCoda(UUID codaId, String filename, BigDecimal total, AccountsRecord account, JsonNode root) {
       JsonNode oldBalanceObject = root.get("old_balance");
       BigDecimal oldBalance = parseCodaAmount(oldBalanceObject.get("old_balance").asLong());
       if (oldBalanceObject.get("old_balance_sign").asText().equals("Debit")) {
           oldBalance = oldBalance.negate();
       }

       BigDecimal currentBalance = account.getCurrentBalance();

       // don't test BigDecimal equality with equals...
       if (currentBalance.compareTo(oldBalance) != 0) {
           throw new IllegalStateException(String.format("missing transactions, current balance is %s, coda %s says it should be %s",
                   currentBalance,
                   filename,
                   oldBalance));
       }

       Iterator<JsonNode> movements = root.get("movements").elements();
       while (movements.hasNext()) {
           JsonNode movement = movements.next();

           if (Integer.parseInt(movement.get("detail_sequence").asText()) != 0) {
               // details about the previous movement, ignoring for now
               continue;
           }

           String sender = movement.get("counterparty_name").asText();
           String senderAccountNumber = movement.get("counterparty_account").asText();
           BigDecimal amount = BigDecimal.valueOf(movement.get("amount").asLong(), 3);
           if (movement.get("amount_sign").asText().equals("Debit")) {
               amount = amount.negate();
           }

           MovementsRecord movementRecord = new MovementsRecord();
           movementRecord.setAccountId(account.getId());
           movementRecord.setAmount(amount);
           movementRecord.setCodaId(codaId);
           movementRecord.setCodaSequenceNumber(Integer.valueOf(movement.get("sequence").asText()));
           movementRecord.setCommunication(movement.get("communication").asText().strip());
           movementRecord.setCounterPartyAccountNumber(senderAccountNumber);
           movementRecord.setCounterPartyName(sender);
           movementRecord.setEntryDate(LocalDate.parse(movement.get("entry_date").asText()));

           movementRepository.createOne(movementRecord);

           total = total.add(amount);
       }


       BigDecimal newBalance = parseCodaAmount(root.get("new_balance").get("new_balance").asLong());
       if (root.get("new_balance").get("new_balance_sign").asText().equals("Debit")) {
           newBalance = newBalance.negate();
       }

       if (total.compareTo(newBalance) != 0) {
           throw new RuntimeException(String.format("new balance after import of %s should be %s, it is %s",
                   filename, newBalance, total));
       }

       accountRepository.updateBalance(account.getId(), total);

       return total;
   }
}
