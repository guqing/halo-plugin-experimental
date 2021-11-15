package run.halo.app.reposiory;

import org.springframework.stereotype.Repository;
import run.halo.app.entity.Potato;
import run.halo.app.repository.base.BaseRepository;

/**
 * @author guqing
 * @since 2021-11-10
 */
@Repository
public interface PotatoRepository extends BaseRepository<Potato, Integer> {

}
