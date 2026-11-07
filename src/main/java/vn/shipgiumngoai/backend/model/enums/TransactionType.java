package vn.shipgiumngoai.backend.model.enums;

public enum TransactionType {
    ESCROW_HOLD,
    ESCROW_TOPUP_DIFF,
    PAY_FROM_CUSTOMER_WALLET,
    REFUND_TO_CUSTOMER_WALLET,
    RELEASE_TO_DRIVER_WALLET,
    DRIVER_GAS_TIP,
    RISK_FUND_COMPENSATION,
    WITHDRAWAL
}
