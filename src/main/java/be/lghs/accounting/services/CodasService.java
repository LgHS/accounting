package be.lghs.accounting.services;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import be.lghs.accounting.model.tables.records.CodasRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.AccountRepository;
import be.lghs.accounting.repositories.CodaRepository;
import be.lghs.accounting.repositories.MovementRepository;
import be.lghs.codaparser.raw.Record;
import be.lghs.codaparser.raw.RecordParser;
import be.lghs.codaparser.raw.records.MovementRecord1;
import be.lghs.codaparser.raw.records.MovementRecord2;
import be.lghs.codaparser.raw.records.MovementRecord3;
import be.lghs.codaparser.raw.records.OldBalanceRecord;
import be.lghs.codaparser.raw.records.model.MovementSign;
import be.lghs.codaparser.raw.records.model.RecordType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodasService {

    private final RecordParser codaParser;
    private final CodaRepository codaRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    @Transactional
    public void handleCodaUpload(UUID accountId, String filename, InputStream content) throws IOException {
        AccountsRecord account = accountRepository.findOne(accountId)
            .orElseThrow(() -> new EmptyResultDataAccessException(1));

        byte[] bytes = StreamUtils.copyToByteArray(content);

        CodasRecord coda = new CodasRecord();
        coda.setFilename(filename);
        coda.setContent(bytes);
        coda.setAccountId(accountId);

        UUID codaId = codaRepository.createOne(coda);

        BigDecimal total = BigDecimal.ZERO;

        Iterator<Record> iterator = codaParser.parse(new ByteArrayInputStream(bytes)).iterator();
        while (iterator.hasNext()) {
            Record record = iterator.next();

            if (record.getType() == RecordType.OLD_BALANCE) {
                OldBalanceRecord oldBalanceRecord = (OldBalanceRecord) record;

                BigDecimal currentBalance = account.getCurrentBalance();
                long currentBalanceInteger = currentBalance.toBigInteger().longValueExact();
                long currentBalanceDecimals = currentBalance
                    .subtract(new BigDecimal(currentBalanceInteger))
                    .multiply(new BigDecimal(100))
                    .longValueExact();

                long oldBalance = Long.parseLong(oldBalanceRecord.getBalance());
                                                                                              // only keep two positions
                long oldBalanceDecimals = Long.parseLong(oldBalanceRecord.getBalanceDecimals().substring(0, 2));

                if (currentBalanceInteger != oldBalance || currentBalanceDecimals != oldBalanceDecimals) {
                    throw new IllegalStateException(String.format("missing transactions, current balance is %s, coda %s says it should be %s.%s",
                        currentBalance,
                        filename,
                        oldBalance,
                        oldBalanceDecimals));
                }
                continue;
            }

            if (record.getType() != RecordType.MOVEMENT) {
                continue;
            }

            MovementRecord1 record1 = (MovementRecord1) record;
            if (record1.getDetailsNumber() != 0) {
                log.warn("unsupported detail line");
                continue;
            }


            MovementRecord2 record2 = record1.getRecord2();
            MovementRecord3 record3 = null;
            if (record2 != null) {
                record3 = record2.getRecord3();
            }
            String sender = null;
            String senderAccountNumber = null;
            if (record3 != null) {
                sender = record3.getCounterPartyName();
                senderAccountNumber = record3.getCounterPartyAccountNumber();
            }

            Money amount = record1.getAmount();
            if (record1.getMovementSign() == MovementSign.DEBIT) {
                amount = amount.negate();
            }

            MovementsRecord movement = new MovementsRecord();
            movement.setAccountId(accountId);
            movement.setAmount(amount.getNumberStripped());
            movement.setCodaId(codaId);
            movement.setCodaSequenceNumber(record1.getSequenceNumber());
            movement.setCommunication(record1.getCommunication());
            movement.setCounterPartyAccountNumber(senderAccountNumber);
            movement.setCounterPartyName(sender);
            movement.setEntryDate(record1.getEntryDate());

            movementRepository.createOne(movement);

            total = total.add(amount.getNumberStripped());
        }

        accountRepository.updateBalance(accountId, total);
    }
}
