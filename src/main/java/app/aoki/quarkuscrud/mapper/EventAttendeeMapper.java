package app.aoki.quarkuscrud.mapper;

import app.aoki.quarkuscrud.entity.EventAttendee;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EventAttendeeMapper {

  @Insert("INSERT INTO event_attendees (event_id, attendee_user_id, meta, created_at, updated_at) VALUES (#{eventId}, #{attendeeUserId}, #{meta}::jsonb, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(EventAttendee eventAttendee);

  @Select("SELECT id, event_id, attendee_user_id, meta::text as meta, created_at, updated_at FROM event_attendees WHERE id = #{id}")
  @Results(
      id = "eventAttendeeResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "eventId", column = "event_id"),
        @Result(property = "attendeeUserId", column = "attendee_user_id"),
        @Result(property = "meta", column = "meta"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
      })
  Optional<EventAttendee> findById(@Param("id") Long id);

  @Select("SELECT id, event_id, attendee_user_id, meta::text as meta, created_at, updated_at FROM event_attendees WHERE event_id = #{eventId}")
  @ResultMap("eventAttendeeResultMap")
  List<EventAttendee> findByEventId(@Param("eventId") Long eventId);

  @Select("SELECT id, event_id, attendee_user_id, meta::text as meta, created_at, updated_at FROM event_attendees WHERE attendee_user_id = #{attendeeUserId}")
  @ResultMap("eventAttendeeResultMap")
  List<EventAttendee> findByAttendeeUserId(@Param("attendeeUserId") Long attendeeUserId);

  @Select("SELECT id, event_id, attendee_user_id, meta::text as meta, created_at, updated_at FROM event_attendees WHERE event_id = #{eventId} AND attendee_user_id = #{attendeeUserId}")
  @ResultMap("eventAttendeeResultMap")
  Optional<EventAttendee> findByEventAndAttendee(
      @Param("eventId") Long eventId, @Param("attendeeUserId") Long attendeeUserId);

  @Update("UPDATE event_attendees SET event_id = #{eventId}, attendee_user_id = #{attendeeUserId}, meta = #{meta}::jsonb, updated_at = #{updatedAt} WHERE id = #{id}")
  void update(EventAttendee eventAttendee);

  @Delete("DELETE FROM event_attendees WHERE id = #{id}")
  void deleteById(@Param("id") Long id);
}
