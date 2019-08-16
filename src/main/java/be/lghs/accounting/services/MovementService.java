package be.lghs.accounting.services;

import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

import static be.lghs.accounting.model.Tables.MOVEMENTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovementService {

    private final MovementRepository movementRepository;

    public MovementsRecord splitMovement(UUID movementId,
                                         String communication,
                                         BigDecimal amount,
                                         UUID categoryId, String communicationSplit,
                                         BigDecimal amountSplit, UUID categorySplitId) {
        MovementsRecord movement = movementRepository.getOne(movementId);
        BigDecimal totalAmount = movement.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) == 0 || amountSplit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("movement amount cannot be 0");
        }

        if (amount.signum() != totalAmount.signum() || amountSplit.signum() != totalAmount.signum()) {
            throw new IllegalArgumentException("cannot change movement sign");
        }

        if (amount.add(amountSplit).compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException(String.format("%s + %s != %s",
                amount, amountSplit, totalAmount));
        }

        movement.setCommunication(communication);
        movement.setAmount(amount);
        movement.setCategoryId(categoryId);
        movement.update(
            MOVEMENTS.COMMUNICATION,
            MOVEMENTS.AMOUNT,
            MOVEMENTS.CATEGORY_ID);

        movementRepository.insertFromTemplate(movement, communicationSplit, amountSplit, categorySplitId);

        return movement;
    }
}
