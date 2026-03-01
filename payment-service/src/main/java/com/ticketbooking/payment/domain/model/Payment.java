package com.ticketbooking.payment.domain.model;

/**
 * Aggregate Root del bounded context de Pagos. POJO puro.
 * Representa el resultado de un intento de pago.
 */
public class Payment {

    private Long bookingId;
    private String transactionId;
    private String status; // SUCCESS, FAILED

    public Payment(Long bookingId, String transactionId, String status) {
        this.bookingId = bookingId;
        this.transactionId = transactionId;
        this.status = status;
    }

    public boolean isSuccessful() { return "SUCCESS".equals(status); }

    public Long getBookingId() { return bookingId; }
    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
}
