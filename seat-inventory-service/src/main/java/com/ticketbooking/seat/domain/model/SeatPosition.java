package com.ticketbooking.seat.domain.model;

import java.util.Objects;

/**
 * Value Object: posición de un asiento dentro del venue.
 * Inmutable. Dos SeatPosition con misma fila y número son iguales.
 */
public final class SeatPosition {

    private final String rowNumber;
    private final int seatNumber;

    public SeatPosition(String rowNumber, int seatNumber) {
        if (rowNumber == null || rowNumber.isBlank()) {
            throw new IllegalArgumentException("Row number cannot be blank");
        }
        if (seatNumber <= 0) {
            throw new IllegalArgumentException("Seat number must be positive");
        }
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
    }

    public String getRowNumber() { return rowNumber; }
    public int getSeatNumber() { return seatNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeatPosition)) return false;
        SeatPosition that = (SeatPosition) o;
        return seatNumber == that.seatNumber && rowNumber.equals(that.rowNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowNumber, seatNumber);
    }

    @Override
    public String toString() {
        return "Row " + rowNumber + " - Seat " + seatNumber;
    }
}
