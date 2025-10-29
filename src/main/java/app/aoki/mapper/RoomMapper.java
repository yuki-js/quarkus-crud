package app.aoki.mapper;

import app.aoki.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoomMapper {
    void insert(Room room);
    Optional<Room> findById(@Param("id") Long id);
    List<Room> findByUserId(@Param("userId") Long userId);
    List<Room> findAll();
    void update(Room room);
    void deleteById(@Param("id") Long id);
}
