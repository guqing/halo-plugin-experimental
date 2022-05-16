package xyz.guqing.plugin.potatoes.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import xyz.guqing.plugin.potatoes.entity.Potato;

/**
 * @author guqing
 * @since 2021-11-09
 */
@Service
public class PotatoService {
    private Map<Integer, Potato> potatosMap = new ConcurrentHashMap<>();

    public PotatoService() {
        Potato potato = new Potato();
        potato.setId(1);
        potato.setName("zhangsan的番茄");
        potatosMap.put(1, potato);
    }

    public Potato create(Potato potato) {
        potatosMap.put(potato.getId(), potato);
        return potato;
    }

    public Potato getById(Integer id) {
        return potatosMap.get(id);
    }
}
