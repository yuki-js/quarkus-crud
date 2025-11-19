package app.aoki.quarkuscrud.mapper;

import app.aoki.quarkuscrud.entity.Friendship;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendshipMapper {

  @Insert(
      "INSERT INTO friendships (sender_user_id, recipient_user_id, created_at, updated_at) "
          + "VALUES (#{senderUserId}, #{recipientUserId}, #{createdAt}, #{updatedAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(Friendship friendship);

  @Select("SELECT id, sender_user_id, recipient_user_id, created_at, updated_at FROM friendships"
      + " WHERE id = #{id}")
  @Results(
      id = "friendshipResultMap",
      value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "senderUserId", column = "sender_user_id"),
        @Result(property = "recipientUserId", column = "recipient_user_id"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
      })
  Optional<Friendship> findById(@Param("id") Long id);

  @Select("SELECT id, sender_user_id, recipient_user_id, created_at, updated_at FROM friendships"
      + " WHERE sender_user_id = #{senderUserId}")
  @ResultMap("friendshipResultMap")
  List<Friendship> findBySenderUserId(@Param("senderUserId") Long senderUserId);

  @Select("SELECT id, sender_user_id, recipient_user_id, created_at, updated_at FROM friendships"
      + " WHERE recipient_user_id = #{recipientUserId}")
  @ResultMap("friendshipResultMap")
  List<Friendship> findByRecipientUserId(@Param("recipientUserId") Long recipientUserId);

  @Select("SELECT id, sender_user_id, recipient_user_id, created_at, updated_at FROM friendships"
      + " WHERE sender_user_id = #{senderUserId} AND recipient_user_id = #{recipientUserId}")
  @ResultMap("friendshipResultMap")
  Optional<Friendship> findBySenderAndRecipient(
      @Param("senderUserId") Long senderUserId, @Param("recipientUserId") Long recipientUserId);
}
