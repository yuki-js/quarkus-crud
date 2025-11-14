package app.aoki.mapper;

import app.aoki.entity.User;
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
public interface UserMapper {

  @Insert(
      "INSERT INTO users (guest_token, created_at, updated_at) VALUES (#{guestToken}, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(User user);

  @Select("SELECT id, guest_token, created_at, updated_at FROM users WHERE id = #{id}")
  @Results(
      id = "userResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "guestToken", column = "guest_token"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
      })
  Optional<User> findById(@Param("id") Long id);

  @Select(
      "SELECT id, guest_token, created_at, updated_at FROM users WHERE guest_token = #{guestToken}")
  @ResultMap("userResultMap")
  Optional<User> findByGuestToken(@Param("guestToken") String guestToken);

  @Update(
      "UPDATE users SET guest_token = #{guestToken}, updated_at = #{updatedAt} WHERE id = #{id}")
  void update(User user);

  @Delete("DELETE FROM users WHERE id = #{id}")
  void deleteById(@Param("id") Long id);
}
