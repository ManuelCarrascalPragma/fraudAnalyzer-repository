package co.com.fraudanalyzer.model.transaction;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Transaction {
    private String id;
    private String accountId;
    private Double amount;
    private String currency;
    private String status;
}
