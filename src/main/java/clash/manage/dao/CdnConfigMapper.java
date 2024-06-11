package clash.manage.dao;

import clash.manage.model.CdnConfig;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CdnConfigMapper {

    List<CdnConfig> list();

    CdnConfig selectByName(String name);
}