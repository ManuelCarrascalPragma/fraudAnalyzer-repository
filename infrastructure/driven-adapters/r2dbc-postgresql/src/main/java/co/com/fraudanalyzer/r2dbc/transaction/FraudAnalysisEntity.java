package co.com.fraudanalyzer.r2dbc.transaction;

import co.com.fraudanalyzer.model.transaction.Transaction;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("fraud_analysis")
public class FraudAnalysisEntity implements Persistable<String> {
    @Id
    @Column("transaction_id")
    private String transactionId;
    
    @Column("account_id")
    private String accountId;
    
    private Double amount;
    private String currency;
    private String status;
    private String reason;
    
    @Column("analyzed_at")
    private LocalDateTime analyzedAt;
    
    @Transient
    @Builder.Default
    private boolean isNew = true;

    public static FraudAnalysisEntity fromDomain(Transaction transaction, String reason) {
        return FraudAnalysisEntity.builder()
                .transactionId(transaction.getId())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .reason(reason)
                .analyzedAt(LocalDateTime.now())
                .isNew(true)
                .build();
    }

    public Transaction toDomain() {
        return Transaction.builder()
                .id(transactionId)
                .accountId(accountId)
                .amount(amount)
                .currency(currency)
                .status(status)
                .build();
    }
    
    @Override
    public String getId() {
        return transactionId;
    }
}
