package xyz.guqing.plugin.potatoes.reposiory;

import org.springframework.stereotype.Repository;
import run.halo.app.repository.base.BaseRepository;
import xyz.guqing.plugin.potatoes.entity.Potato;

/**
 * @author guqing
 * @since 2021-11-10
 */
@Repository
public interface PotatoRepository extends BaseRepository<Potato, Integer> {
    Potato findPotatoByName(String name);
}
