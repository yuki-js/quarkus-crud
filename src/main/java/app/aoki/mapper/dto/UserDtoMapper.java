package app.aoki.mapper.dto;

import app.aoki.entity.User;
import app.aoki.generated.model.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between User entities and generated DTOs. Uses CDI
 * (@ApplicationScoped) for injection into Quarkus beans.
 */
@Mapper(componentModel = "cdi")
@ApplicationScoped
public interface UserDtoMapper {

  /**
   * Converts a User entity to a UserResponse DTO. Note: guestToken is NOT included in the response
   * for security reasons.
   *
   * @param user the User entity
   * @return the UserResponse DTO
   */
  @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(user.getCreatedAt()))")
  UserResponse toUserResponse(User user);

  /**
   * Helper method to convert LocalDateTime to OffsetDateTime. Generated models use OffsetDateTime
   * while entities use LocalDateTime.
   *
   * @param localDateTime the LocalDateTime to convert
   * @return the OffsetDateTime at UTC offset
   */
  default OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    return localDateTime.atOffset(ZoneOffset.UTC);
  }
}
