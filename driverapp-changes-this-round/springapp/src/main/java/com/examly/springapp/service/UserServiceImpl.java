package com.examly.springapp.service;

import com.examly.springapp.dto.UserDTO;
import com.examly.springapp.exceptions.InvalidRequestException;
import com.examly.springapp.model.Roles;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private OtpService otpService;

    @Value("${otp.verification-required:true}")
    private boolean otpRequired;

    @Override
    public UserDTO createUser(UserDTO userDto) {
        if (userRepo.findByEmail(userDto.getEmail()).isPresent()) {
            return null; // Signals duplicate email
        }
        if (otpRequired) {
            if (!otpService.isEmailVerified(userDto.getEmail())) {
                throw new InvalidRequestException("Please verify your email address with the OTP before signing up.");
            }
            if (!otpService.isMobileVerified(userDto.getMobileNumber())) {
                throw new InvalidRequestException("Please verify your mobile number with the OTP before signing up.");
            }
        }
        UserDTO created = save(userDto, Roles.CUSTOMER);
        otpService.clearVerification(userDto.getEmail(), userDto.getMobileNumber());
        return created;
    }

    @Override
    public UserDTO createAdmin(UserDTO userDto) {
        if (userRepo.findByEmail(userDto.getEmail()).isPresent()) {
            return null; // Signals duplicate email
        }
        return save(userDto, Roles.ADMIN);
    }

    private UserDTO save(UserDTO userDto, String role) {
        User user = modelMapper.map(userDto, User.class);
        user.setUserId(null);          // a client may never choose (or overwrite) an id
        user.setUserRole(role);        // the role is decided by the server, never by the request
        user.setEmail(userDto.getEmail().trim());
        user.setUsername(userDto.getUsername().trim());
        user.setPassword(encoder.encode(userDto.getPassword()));
        return modelMapper.map(userRepo.save(user), UserDTO.class);
    }

    @Override
    public boolean emailExists(String email) {
        return userRepo.findByEmail(email.trim()).isPresent();
    }

    @Override
    public UserDTO loginUser(UserDTO userDto) {
        // Will be handled via Spring Security and JwtUtils in AuthController
        Optional<User> user = userRepo.findByEmail(userDto.getEmail());
        return user.map(found -> modelMapper.map(found, UserDTO.class)).orElse(null);
    }
}
