package clash.manage.dao;

import clash.manage.model.ClashProxies;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClashProxiesMapper {

    List<ClashProxies> list();

    List<ClashProxies> listBySubscribeName(String subscribeName);

    int insert(ClashProxies clashProxies);

    int update(ClashProxies clashProxies);

}