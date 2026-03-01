package com.ticketbooking.common.exception;

public class SeatNotAvailableException extends RuntimeException {
    public SeatNotAvailableException(Long seatId) {
        super("Seat " + seatId + " is not available for booking");
    }
}
