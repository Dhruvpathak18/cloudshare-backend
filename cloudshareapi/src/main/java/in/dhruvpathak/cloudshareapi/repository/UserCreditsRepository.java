package in.dhruvpathak.cloudshareapi.repository;

import in.dhruvpathak.cloudshareapi.document.UserCredits;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserCreditsRepository extends MongoRepository<UserCredits,String> {
    Optional<UserCredits> findByClerkId(String clerkId);
}
