package app.aoki.mapper;

import app.aoki.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.Optional;

@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO users (guest_token, created_at, updated_at) VALUES (#{guestToken}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Select("SELECT id, guest_token AS guestToken, created_at AS createdAt, updated_at AS updatedAt FROM users WHERE id = #{id}")
    Optional<User> findById(@Param("id") Long id);
    
    @Select("SELECT id, guest_token AS guestToken, created_at AS createdAt, updated_at AS updatedAt FROM users WHERE guest_token = #{guestToken}")
    Optional<User> findByGuestToken(@Param("guestToken") String guestToken);
    
    @Update("UPDATE users SET guest_token = #{guestToken}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
}
