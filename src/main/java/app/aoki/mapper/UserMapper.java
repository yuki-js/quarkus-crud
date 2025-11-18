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
      "INSERT INTO users (auth_identifier, created_at, updated_at) VALUES (#{authIdentifier}, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(User user);

  @Select("SELECT id, auth_identifier, created_at, updated_at FROM users WHERE id = #{id}")
  @Results(
      id = "userResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "authIdentifier", column = "auth_identifier"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
      })
  Optional<User> findById(@Param("id") Long id);

  @Select(
      "SELECT id, auth_identifier, created_at, updated_at FROM users WHERE auth_identifier = #{authIdentifier}")
  @ResultMap("userResultMap")
  Optional<User> findByAuthIdentifier(@Param("authIdentifier") String authIdentifier);

  @Update(
      "UPDATE users SET auth_identifier = #{authIdentifier}, updated_at = #{updatedAt} WHERE id = #{id}")
  void update(User user);

  @Delete("DELETE FROM users WHERE id = #{id}")
  void deleteById(@Param("id") Long id);
}
