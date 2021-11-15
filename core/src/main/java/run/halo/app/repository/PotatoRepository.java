package run.halo.app.repository;

import org.springframework.stereotype.Repository;
import run.halo.app.model.entity.Post;
import run.halo.app.repository.base.BaseRepository;

/**
 * @author guqing
 * @since 2021-11-10
 */
@Repository
public interface PotatoRepository extends BaseRepository<Post, Integer> {
}
