package be.heh.clipclap.users.internal;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import be.heh.clipclap.users.UserApi;

@Service
class UserApiImpl implements UserApi {

	private final UserRepository repository;

	UserApiImpl(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<Long> findIdBySub(String sub) {
		return repository.findIdBySub(sub);
	}

	@Override
	public Set<Long> findSuspendedIds(Set<Long> userIds) {
		return repository.findSuspendedIds(userIds);
	}

	@Override
	public Set<Long> findAllSuspendedIds() {
		return repository.findAllSuspendedIds();
	}

	@Override
	public Map<Long, String> findEmails(Set<Long> userIds) {
		return repository.findEmails(userIds);
	}

}
