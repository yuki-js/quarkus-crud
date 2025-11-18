package app.aoki.quarkuscrud.mapper;

import app.aoki.quarkuscrud.entity.Room;
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
public interface RoomMapper {

  @Insert(
      "INSERT INTO rooms (name, description, user_id, created_at, updated_at) "
          + "VALUES (#{name}, #{description}, #{userId}, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(Room room);

  @Select(
      "SELECT id, name, description, user_id, created_at, updated_at FROM rooms WHERE id = #{id}")
  @Results(
      id = "roomResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
      })
  Optional<Room> findById(@Param("id") Long id);

  @Select(
      "SELECT id, name, description, user_id, created_at, updated_at FROM rooms WHERE user_id ="
          + " #{userId}")
  @ResultMap("roomResultMap")
  List<Room> findByUserId(@Param("userId") Long userId);

  @Select(
      "SELECT id, name, description, user_id, created_at, updated_at FROM rooms ORDER BY"
          + " created_at DESC")
  @ResultMap("roomResultMap")
  List<Room> findAll();

  @Update(
      "UPDATE rooms SET name = #{name}, description = #{description}, user_id = #{userId},"
          + " updated_at = #{updatedAt} WHERE id = #{id}")
  void update(Room room);

  @Delete("DELETE FROM rooms WHERE id = #{id}")
  void deleteById(@Param("id") Long id);
}
