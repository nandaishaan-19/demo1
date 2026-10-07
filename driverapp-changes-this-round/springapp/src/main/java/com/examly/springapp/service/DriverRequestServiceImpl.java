package com.examly.springapp.service;

import com.examly.springapp.dto.DriverRequestDTO;
import com.examly.springapp.exceptions.DriverRequestDeletionException;
import com.examly.springapp.exceptions.InvalidRequestException;
import com.examly.springapp.model.Driver;
import com.examly.springapp.model.DriverRequest;
import com.examly.springapp.repository.DriverRequestRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DriverRequestServiceImpl implements DriverRequestService {

    /** A trip can only be cancelled up to this long before it starts. */
    public static final Duration CANCEL_NOTICE = Duration.ofHours(24);

    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm");

    @Autowired
    private Clock clock;

    @Autowired
    private DriverRequestRepo driverRequestRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public DriverRequestDTO addDriverRequest(DriverRequestDTO driverRequestDto) {
        DriverRequest driverRequest = modelMapper.map(driverRequestDto, DriverRequest.class);
        if (driverRequest.getRequestDate() == null) {
            driverRequest.setRequestDate(LocalDate.now());
        }
        if (driverRequest.getStatus() == null) {
            driverRequest.setStatus("Pending"); // a new request always starts as Pending
        }
        return modelMapper.map(driverRequestRepo.save(driverRequest), DriverRequestDTO.class);
    }

    @Override
    public Optional<DriverRequestDTO> getDriverRequestById(Long driverRequestId) {
        return driverRequestRepo.findById(driverRequestId).map(request -> modelMapper.map(request, DriverRequestDTO.class));
    }

    @Override
    public List<DriverRequestDTO> getAllDriverRequests() {
        return toDtos(driverRequestRepo.findAll());
    }

    @Override
    public DriverRequestDTO updateDriverRequest(Long driverRequestId, DriverRequestDTO driverRequest) {
        Optional<DriverRequest> existingReqOpt = driverRequestRepo.findById(driverRequestId);
        if (existingReqOpt.isPresent()) {
            DriverRequest existingReq = existingReqOpt.get();
            if (driverRequest.getStatus() != null) {
                checkStatusChange(existingReq, driverRequest.getStatus());
                existingReq.setStatus(driverRequest.getStatus());
            }
            if (driverRequest.getTripDate() != null) existingReq.setTripDate(driverRequest.getTripDate());
            if (driverRequest.getTimeSlot() != null) existingReq.setTimeSlot(driverRequest.getTimeSlot());
            if (driverRequest.getPickupLocation() != null) existingReq.setPickupLocation(driverRequest.getPickupLocation());
            if (driverRequest.getDropLocation() != null) existingReq.setDropLocation(driverRequest.getDropLocation());
            if (driverRequest.getEstimatedDuration() != null) existingReq.setEstimatedDuration(driverRequest.getEstimatedDuration());
            if (driverRequest.getPaymentAmount() != null) existingReq.setPaymentAmount(driverRequest.getPaymentAmount());
            if (driverRequest.getComments() != null) existingReq.setComments(driverRequest.getComments());
            if (driverRequest.getActualDropTime() != null) existingReq.setActualDropTime(driverRequest.getActualDropTime());
            if (driverRequest.getActualDropDate() != null) existingReq.setActualDropDate(driverRequest.getActualDropDate());
            if (driverRequest.getActualDuration() != null) existingReq.setActualDuration(driverRequest.getActualDuration());
            if (driverRequest.getDriver() != null) existingReq.setDriver(modelMapper.map(driverRequest.getDriver(), Driver.class));
            return modelMapper.map(driverRequestRepo.save(existingReq), DriverRequestDTO.class);
        }
        return null;
    }

    @Override
    public DriverRequestDTO deleteDriverRequest(Long driverRequestId) {
        Optional<DriverRequest> existingReqOpt = driverRequestRepo.findById(driverRequestId);
        if (existingReqOpt.isPresent()) {
            try {
                driverRequestRepo.delete(existingReqOpt.get());
                return modelMapper.map(existingReqOpt.get(), DriverRequestDTO.class);
            } catch (Exception e) {
                throw new DriverRequestDeletionException("Failed to delete driver request with ID: " + driverRequestId);
            }
        }
        return null;
    }

    @Override
    public List<DriverRequestDTO> findDriverRequestsByUserId(Long userId) {
        return toDtos(driverRequestRepo.findByUserUserId(userId));
    }

    @Override
    public List<DriverRequestDTO> findDriverRequestsByDriverId(Long driverId) {
        return toDtos(driverRequestRepo.findByDriverDriverId(driverId));
    }

    private List<DriverRequestDTO> toDtos(List<DriverRequest> requests) {
        return requests.stream().map(request -> modelMapper.map(request, DriverRequestDTO.class)).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ trip rules

    /** The moment the trip starts (trip date + time slot), or null when one of them is missing. */
    private LocalDateTime tripStart(DriverRequest request) {
        if (request.getTripDate() == null || request.getTimeSlot() == null) {
            return null;
        }
        return LocalDateTime.of(request.getTripDate(), request.getTimeSlot());
    }

    /**
     * Rules for the status changes that depend on the clock:
     * <ul>
     *   <li>"Trip End": only an Approved trip, and only once its start time has been reached;</li>
     *   <li>"Cancelled": only a Pending or Approved trip, and only until 24 hours before it starts.</li>
     * </ul>
     * Setting the status a request already has is always fine (nothing changes).
     */
    private void checkStatusChange(DriverRequest request, String newStatus) {
        if (newStatus.equals(request.getStatus())) {
            return;
        }
        LocalDateTime start = tripStart(request);
        LocalDateTime now = LocalDateTime.now(clock);
        if ("Trip End".equals(newStatus)) {
            if (!"Approved".equals(request.getStatus())) {
                throw new InvalidRequestException("Only an approved trip can be ended.");
            }
            if (start != null && now.isBefore(start)) {
                throw new InvalidRequestException("The trip cannot be ended before it starts (it starts on "
                        + start.format(START_FORMAT) + ").");
            }
        } else if ("Cancelled".equals(newStatus)) {
            if (!"Pending".equals(request.getStatus()) && !"Approved".equals(request.getStatus())) {
                throw new InvalidRequestException("Only a pending or approved trip can be cancelled.");
            }
            if (start != null && now.isAfter(start.minus(CANCEL_NOTICE))) {
                throw new InvalidRequestException("A trip can be cancelled only up to 24 hours before it starts (it starts on "
                        + start.format(START_FORMAT) + ").");
            }
        }
    }
}
