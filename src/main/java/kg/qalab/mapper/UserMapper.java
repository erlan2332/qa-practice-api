package kg.qalab.mapper;

import kg.qalab.dto.user.UserResponse;
import kg.qalab.model.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
            user.id(),
            user.email(),
            user.name(),
            user.role().name()
        );
    }
}
