package database.repository;

import database.models.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    @Query("SELECT l FROM Link l WHERE l.token = :token")
    List<Link> findByToken(@Param("token") String token);

    @Query("SELECT l FROM Link l WHERE l.project.id = :id")
    List<Link> findLinksByProjectID(@Param("id") Long id);
    @Query("SELECT l FROM Link l WHERE l.user.id = :id")
    List<Link> findLinksByUserID(@Param("id") Long id);
}
