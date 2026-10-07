package com.examly.springapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import com.examly.springapp.dto.validation.OnCreate;
import com.examly.springapp.dto.validation.ValidationPatterns;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Data Transfer Object for {@link com.examly.springapp.model.DriverRequest}.
 * POST validates everything (group OnCreate); PUT validates only the fields that are present.
 * The related user and driver travel as nested {@link UserDTO} / {@link DriverDTO} objects,
 * exactly like the JSON the Angular app sends: {@code { "user": {"userId": 1}, "driver": {"driverId": 2}, ... }}.
 */
public class DriverRequestDTO {
    private Long driverRequestId;
    @NotNull(groups = OnCreate.class, message = "User is required")
    private UserDTO user;

    @NotNull(groups = OnCreate.class, message = "Driver is required")
    private DriverDTO driver;

    private LocalDate requestDate;

    // optional: a new request defaults to "Pending"
    @Pattern(regexp = ValidationPatterns.REQUEST_STATUS, message = "Status must be Pending, Approved, Rejected, Cancelled, Trip End or Closed")
    private String status;

    @NotNull(groups = OnCreate.class, message = "Trip date is required")
    @FutureOrPresent(groups = OnCreate.class, message = "Trip date cannot be in the past")
    private LocalDate tripDate;

    @NotNull(groups = OnCreate.class, message = "Time slot is required")
    private LocalTime timeSlot;

    // Required when a request is created. On an update (partial) a field may be left out, but it
    // can never be sent blank.
    @NotNull(groups = OnCreate.class, message = "Pickup location is required")
    @Pattern(regexp = ValidationPatterns.NOT_BLANK_TEXT, message = "Pickup location is required")
    private String pickupLocation;

    @NotNull(groups = OnCreate.class, message = "Drop location is required")
    @Pattern(regexp = ValidationPatterns.NOT_BLANK_TEXT, message = "Drop location is required")
    private String dropLocation;

    @NotNull(groups = OnCreate.class, message = "Estimated duration is required")
    @Pattern(regexp = ValidationPatterns.NOT_BLANK_TEXT, message = "Estimated duration is required")
    private String estimatedDuration;

    @PositiveOrZero(message = "Payment amount cannot be negative")
    private Double paymentAmount;
    private String comments;
    private LocalTime actualDropTime;
    private LocalDate actualDropDate;
    private String actualDuration;

    public DriverRequestDTO() {
    }

    public Long getDriverRequestId() {
        return driverRequestId;
    }

    public void setDriverRequestId(Long driverRequestId) {
        this.driverRequestId = driverRequestId;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public DriverDTO getDriver() {
        return driver;
    }

    public void setDriver(DriverDTO driver) {
        this.driver = driver;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public void setTripDate(LocalDate tripDate) {
        this.tripDate = tripDate;
    }

    public LocalTime getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(LocalTime timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropLocation() {
        return dropLocation;
    }

    public void setDropLocation(String dropLocation) {
        this.dropLocation = dropLocation;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalTime getActualDropTime() {
        return actualDropTime;
    }

    public void setActualDropTime(LocalTime actualDropTime) {
        this.actualDropTime = actualDropTime;
    }

    public LocalDate getActualDropDate() {
        return actualDropDate;
    }

    public void setActualDropDate(LocalDate actualDropDate) {
        this.actualDropDate = actualDropDate;
    }

    public String getActualDuration() {
        return actualDuration;
    }

    public void setActualDuration(String actualDuration) {
        this.actualDuration = actualDuration;
    }

    // ---- checks of the nested ids (only used by validation, never part of the JSON) ----

    @JsonIgnore
    @AssertTrue(groups = OnCreate.class, message = "user.userId is required")
    public boolean isUserIdProvided() {
        return user == null || user.getUserId() != null;
    }

    @JsonIgnore
    @AssertTrue(groups = OnCreate.class, message = "driver.driverId is required")
    public boolean isDriverIdProvided() {
        return driver == null || driver.getDriverId() != null;
    }
}
