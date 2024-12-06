package database.repository;

import database.models.Project;
import database.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT p FROM User p WHERE p.email = :email")
    List<User> findByUserEmail(@Param("email") String email);
}
