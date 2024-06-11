package clash.manage.dao;

import clash.manage.model.ClashSubscribeConfig;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClashSubscribeConfigMapper {

    int update(ClashSubscribeConfig clashSubscribeConfig);

    List<ClashSubscribeConfig> list();

}