package com.examly.springapp.config;

import com.examly.springapp.dto.UserDTO;
import com.examly.springapp.model.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one {@link ModelMapper} used everywhere an entity is turned into a DTO or a DTO into an entity
 * (it replaces the hand-written DtoMapper class).
 *
 * <ul>
 *   <li>STRICT matching: a destination property is filled only from the source property with exactly the
 *       same name and path. Without it ModelMapper could, for example, fill DriverRequest.driverRequestId from
 *       driver.driverId because both end in "Id".</li>
 *   <li>The encoded password of a {@link User} is never copied into a {@link UserDTO}.</li>
 * </ul>
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(false)
                .setAmbiguityIgnored(false);

        // User -> UserDTO: leave the password out (UserDTO.password is also write-only in JSON).
        modelMapper.typeMap(User.class, UserDTO.class)
                .addMappings(mapping -> mapping.skip(UserDTO::setPassword));

        return modelMapper;
    }
}
