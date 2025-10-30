package app.aoki.mapper;

import app.aoki.entity.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

  void insert(User user);

  Optional<User> findById(@Param("id") Long id);

  Optional<User> findByGuestToken(@Param("guestToken") String guestToken);

  void update(User user);

  void deleteById(@Param("id") Long id);
}
