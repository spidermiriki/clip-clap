package be.heh.clipclap.users.internal;

import java.util.Optional;

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

}
