package app.aoki.mapper;

import app.aoki.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO users (guest_token, created_at, updated_at) VALUES (#{guestToken}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Select("SELECT id, guest_token, created_at, updated_at FROM users WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "guestToken", column = "guest_token"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<User> findById(@Param("id") Long id);
    
    @Select("SELECT id, guest_token, created_at, updated_at FROM users WHERE guest_token = #{guestToken}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "guestToken", column = "guest_token"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<User> findByGuestToken(@Param("guestToken") String guestToken);
    
    @Update("UPDATE users SET guest_token = #{guestToken}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
}
