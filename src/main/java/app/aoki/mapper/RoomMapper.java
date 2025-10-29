package app.aoki.mapper;

import app.aoki.entity.Room;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoomMapper {
    
    @Insert("INSERT INTO rooms (name, description, user_id, created_at, updated_at) VALUES (#{name}, #{description}, #{userId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Room room);
    
    @Select("SELECT id, name, description, user_id, created_at, updated_at FROM rooms WHERE id = #{id}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Room> findById(@Param("id") Long id);
    
    @Select("SELECT id, name, description, user_id, created_at, updated_at FROM rooms WHERE user_id = #{userId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<Room> findByUserId(@Param("userId") Long userId);
    
    @Select("SELECT id, name, description, user_id, created_at, updated_at FROM rooms ORDER BY created_at DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "description", column = "description"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<Room> findAll();
    
    @Update("UPDATE rooms SET name = #{name}, description = #{description}, user_id = #{userId}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(Room room);
    
    @Delete("DELETE FROM rooms WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
}
