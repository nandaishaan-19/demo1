package com.examly.springapp.service;

import com.examly.springapp.dto.DriverDTO;
import com.examly.springapp.exceptions.DriverDeletionException;
import com.examly.springapp.exceptions.DuplicateDriverException;
import com.examly.springapp.model.Driver;
import com.examly.springapp.repository.DriverRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverRepo driverRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public DriverDTO addDriver(DriverDTO driverDto) {
        Optional<Driver> existingDriver = driverRepo.findByLicenseNumber(driverDto.getLicenseNumber());
        if (existingDriver.isPresent()) {
            throw new DuplicateDriverException("Driver with license number " + driverDto.getLicenseNumber() + " already exists.");
        }
        Driver driver = modelMapper.map(driverDto, Driver.class);
        if (driver.getAvailabilityStatus() == null) {
            driver.setAvailabilityStatus("Active"); // a new driver starts as Active
        }
        return modelMapper.map(driverRepo.save(driver), DriverDTO.class);
    }

    @Override
    public Optional<DriverDTO> getDriverById(Long driverId) {
        return driverRepo.findById(driverId).map(driver -> modelMapper.map(driver, DriverDTO.class));
    }

    @Override
    public List<DriverDTO> getAllDrivers() {
        return driverRepo.findAll().stream().map(driver -> modelMapper.map(driver, DriverDTO.class)).collect(Collectors.toList());
    }

    @Override
    public DriverDTO updateDriver(Long driverId, DriverDTO driverDto) {
        Optional<Driver> existingDriverOpt = driverRepo.findById(driverId);
        if (existingDriverOpt.isPresent()) {
            Driver existingDriver = existingDriverOpt.get();
            existingDriver.setDriverName(driverDto.getDriverName());
            existingDriver.setLicenseNumber(driverDto.getLicenseNumber());
            existingDriver.setExperienceYears(driverDto.getExperienceYears());
            existingDriver.setContactNumber(driverDto.getContactNumber());
            if (driverDto.getAvailabilityStatus() != null) {
                existingDriver.setAvailabilityStatus(driverDto.getAvailabilityStatus());
            }
            existingDriver.setAddress(driverDto.getAddress());
            existingDriver.setVehicleType(driverDto.getVehicleType());
            existingDriver.setHourlyRate(driverDto.getHourlyRate());
            existingDriver.setImage(driverDto.getImage());
            return modelMapper.map(driverRepo.save(existingDriver), DriverDTO.class);
        }
        return null;
    }

    @Override
    public DriverDTO deleteDriver(Long driverId) {
        Optional<Driver> existingDriver = driverRepo.findById(driverId);
        if (existingDriver.isPresent()) {
            try {
                driverRepo.delete(existingDriver.get());
                return modelMapper.map(existingDriver.get(), DriverDTO.class);
            } catch (Exception e) {
                throw new DriverDeletionException("Failed to delete driver with ID: " + driverId);
            }
        }
        return null;
    }
}
