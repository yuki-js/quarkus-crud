package app.aoki.quarkuscrud.mapper;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserLifecycleStatus;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;

@Mapper
public interface UserMapper {

  @Insert(
      "INSERT INTO users (created_at, updated_at, lifecycle_status, current_profile_card_revision,"
          + " meta) "
          + "VALUES (#{createdAt}, #{updatedAt}, #{lifecycleStatus,"
          + " typeHandler=org.apache.ibatis.type.EnumTypeHandler}, #{currentProfileCardRevision},"
          + " #{meta}::jsonb)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(User user);

  @Select(
      "SELECT id, created_at, updated_at, lifecycle_status, current_profile_card_revision, meta"
          + " FROM users WHERE id = #{id}")
  @Results(
      id = "userResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(
            property = "lifecycleStatus",
            column = "lifecycle_status",
            javaType = UserLifecycleStatus.class,
            typeHandler = EnumTypeHandler.class),
        @Result(
            property = "currentProfileCardRevision",
            column = "current_profile_card_revision",
            jdbcType = JdbcType.BIGINT),
        @Result(property = "meta", column = "meta")
      })
  Optional<User> findById(@Param("id") Long id);

  @Update(
      "UPDATE users SET updated_at = #{updatedAt}, lifecycle_status = #{lifecycleStatus,"
          + " typeHandler=org.apache.ibatis.type.EnumTypeHandler}, current_profile_card_revision ="
          + " #{currentProfileCardRevision}, meta = #{meta}::jsonb WHERE id = #{id}")
  void update(User user);
}
