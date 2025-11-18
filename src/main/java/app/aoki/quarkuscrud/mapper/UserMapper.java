package app.aoki.quarkuscrud.mapper;

import app.aoki.quarkuscrud.entity.AuthenticationProvider;
import app.aoki.quarkuscrud.entity.User;
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
import org.apache.ibatis.type.EnumTypeHandler;

@Mapper
public interface UserMapper {

  @Insert(
      "INSERT INTO users (auth_identifier, auth_provider, external_subject, created_at, updated_at) "
          + "VALUES (#{authIdentifier}, #{authProvider, typeHandler=org.apache.ibatis.type.EnumTypeHandler}, #{externalSubject}, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(User user);

  @Select(
      "SELECT id, auth_identifier, auth_provider, external_subject, created_at, updated_at FROM users WHERE id = #{id}")
  @Results(
      id = "userResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "authIdentifier", column = "auth_identifier"),
        @Result(
            property = "authProvider",
            column = "auth_provider",
            javaType = AuthenticationProvider.class,
            typeHandler = EnumTypeHandler.class),
        @Result(property = "externalSubject", column = "external_subject"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
      })
  Optional<User> findById(@Param("id") Long id);

  @Select(
      "SELECT id, auth_identifier, auth_provider, external_subject, created_at, updated_at FROM users WHERE auth_identifier = #{authIdentifier}")
  @ResultMap("userResultMap")
  Optional<User> findByAuthIdentifier(@Param("authIdentifier") String authIdentifier);

  @Select(
      "SELECT id, auth_identifier, auth_provider, external_subject, created_at, updated_at FROM users "
          + "WHERE auth_provider = #{authProvider, typeHandler=org.apache.ibatis.type.EnumTypeHandler} "
          + "AND external_subject = #{externalSubject}")
  @ResultMap("userResultMap")
  Optional<User> findByProviderAndExternalSubject(
      @Param("authProvider") AuthenticationProvider authProvider,
      @Param("externalSubject") String externalSubject);

  @Update(
      "UPDATE users SET auth_identifier = #{authIdentifier}, auth_provider = #{authProvider, typeHandler=org.apache.ibatis.type.EnumTypeHandler}, "
          + "external_subject = #{externalSubject}, updated_at = #{updatedAt} WHERE id = #{id}")
  void update(User user);

  @Delete("DELETE FROM users WHERE id = #{id}")
  void deleteById(@Param("id") Long id);
}
