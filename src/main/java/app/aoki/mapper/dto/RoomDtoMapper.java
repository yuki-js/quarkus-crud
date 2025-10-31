package app.aoki.mapper.dto;

import app.aoki.entity.Room;
import app.aoki.generated.model.CreateRoomRequest;
import app.aoki.generated.model.RoomResponse;
import app.aoki.generated.model.UpdateRoomRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting between Room entities and generated DTOs. Uses CDI
 * (@ApplicationScoped) for injection into Quarkus beans.
 */
@Mapper(componentModel = "cdi")
@ApplicationScoped
public interface RoomDtoMapper {

  /**
   * Converts a Room entity to a RoomResponse DTO.
   *
   * @param room the Room entity
   * @return the RoomResponse DTO
   */
  @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(room.getCreatedAt()))")
  RoomResponse toRoomResponse(Room room);

  /**
   * Converts a CreateRoomRequest DTO to a Room entity. Note: id, createdAt, updatedAt are not set
   * by this mapping and must be set by the service layer.
   *
   * @param request the CreateRoomRequest DTO
   * @return the Room entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Room toRoom(CreateRoomRequest request);

  /**
   * Updates an existing Room entity from an UpdateRoomRequest DTO. Only updates name and
   * description fields.
   *
   * @param request the UpdateRoomRequest DTO
   * @param room the Room entity to update
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateRoomFromDto(UpdateRoomRequest request, @MappingTarget Room room);

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
