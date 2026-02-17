CREATE TABLE IF NOT EXISTS fraud_analysis (
    transaction_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_analysis_status ON fraud_analysis(status);
CREATE INDEX idx_fraud_analysis_analyzed_at ON fraud_analysis(analyzed_at);
