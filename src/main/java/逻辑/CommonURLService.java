package 逻辑;

import com.goodwill.core.orm.MatchType;
import com.goodwill.core.orm.PropertyFilter;
import com.goodwill.hadoop.common.modal.ResultVO;
import com.goodwill.hadoop.solr.SolrQueryUtils;
import com.goodwill.hdr.civ.base.dao.HbaseDao;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.config.ConfigCache;
import com.goodwill.hdr.civ.enums.HdrConstantEnum;
import com.goodwill.hdr.civ.enums.HdrTableEnum;
import com.goodwill.hdr.civ.utils.GenUtils;
import com.goodwill.hdr.civ.utils.SolrUtils;
import com.goodwill.hdr.civ.utils.Utils;
import com.goodwill.hdr.civ.web.entity.CommonConfig;
import com.goodwill.hdr.civ.web.service.CommonURLService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author guozhenlei
 * @Description 类描述：
 * @Date 2019-12-09 15:04
 * @modify 修改记录：
 */
@Service
public class CommonURLService implements CommonURLService {
    private static Logger logger = LoggerFactory.getLogger(CommonURLService.class);
    @Autowired
    private HbaseDao hbaseDao;

	@Override
	public Map<String, String> getCommonUrl(String patientId, String visitId, String sysCode,
			Map<String, String> paramMap) {
		// TODO Auto-generated method stub
		Map<String, String> rs = new HashMap<String, String>();
		String project = CommonConfig.getProject_site();
		if (StringUtils.isBlank(project)) {
			return rs;
		}
		String url = CommonConfig.getURL(sysCode);//获取系统URL
		if (StringUtils.isBlank(url)) {
			rs.put("status", "0");
			rs.put("msg", "请联系管理员，请配置调用地址。");
			return rs;
		}
		//南医三院特殊处理
		if (HdrConstantEnum.HOSPITAL_NYSY.getCode().equals(ConfigCache.getCache("org_oid")) && sysCode.contains("NURSE")) {
			//        if(true){
			String res = GenUtils.setUrl(url, patientId, visitId, paramMap);
			rs.put("status", "1");
			rs.put("msg", "获取URL成功");
			rs.put("url", res);
			return rs;
		}
        try {
            List<String> params = CommonConfig.getParams(sysCode);
            Map<String, String> config = CommonConfig.getParam_configs(sysCode);
            Map<String, String> values = getParamValue(patientId, visitId, sysCode, paramMap, config);
            System.out.println("*****1:"+url);
            for (int i = 0; i < params.size(); i++) {
                String field = params.get(i);
                String value = values.get(field);
                System.out.println("field:"+field);
                System.out.println("field:"+field.length());
                System.out.println("value:"+value);
                //千佛山特殊处理新增是否加密
                if ("WEBEMR".equals(sysCode)  && HdrConstantEnum.HOSPITAL_QFS.getCode().equals(ConfigCache.getCache("org_oid"))) {
                    value = Utils.UrlEncodeBase64(value);
                }
                if (StringUtils.isNotBlank(value)) {
                    url = url.replace("#{" + field + "}", value);
                    System.out.println("*****:"+field+":"+url);
                } else {
                    rs.put("status", "0");
                    rs.put("msg", "请联系管理员，" + field + "参数为空。");
                    return rs;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("*****:3"+url);
		rs.put("status", "1");
		rs.put("linkType", CommonConfig.getLinkType(sysCode));
		rs.put("url", url);
		return rs;
	}

    private Map<String, String> getParamValue(String patientId, String visitId, String sysCode,
                                              Map<String, String> mapParam, Map<String, String> configs) {
        // TODO Auto-generated method stub
        Map<String, String> rs = new HashMap<String, String>();
        for (String key : configs.keySet()) {
            //如果此参数已获取不需要查询
            if (mapParam.keySet().contains(key)) {
                rs.put(key, mapParam.get(key));
                continue;
            }
            if (StringUtils.isNotBlank(patientId) && "patientId".equalsIgnoreCase(key)) {
                rs.put(key, patientId);
                continue;
            }
            if (StringUtils.isNotBlank(visitId) && "visitId".equalsIgnoreCase(key)) {
                rs.put(key, visitId);
                continue;
            }
            //需要查询
            String[] config = configs.get(key).split(",");
            List<PropertyFilter> filters = new ArrayList<PropertyFilter>();
            List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            //没有rowkey时构造查询条件
            if (StringUtils.isBlank(mapParam.get("ROWKEY")) && !HdrTableEnum.HDR_PATIENT.getCode().equals(config[1])) {
                boolean isNeeded = true;
                if( config.length > 2) {
                    try {
                        isNeeded = Boolean.valueOf(config[2]);
                    }catch(Exception e){
                        logger.error("外置url配置查询字段是否当做查询条件配置错误，需要为true或者false");
                        e.printStackTrace();
                    }
                }
                if (StringUtils.isNotBlank(visitId) && isNeeded) {
                    filters.add(new PropertyFilter("VISIT_ID", "STRING", MatchType.EQ.getOperation(), visitId));
                }
                //TODO 这段逻辑添加原因不明，暂时注释掉
//                for (Map.Entry entry : mapParam.entrySet()) {
//                    if (StringUtils.isNotBlank((String) entry.getKey())) {
//                        filters.add(new PropertyFilter((String) entry.getKey(), "STRING", MatchType.EQ.getOperation(),
//                                (String) entry.getValue()));
//                    }
//                }
            }
            //无rowkey查询
            if (StringUtils.isBlank(mapParam.get("ROWKEY"))) {
                list = hbaseDao.findConditionByPatient(config[1], patientId, filters, config[0]);
            } else if (StringUtils.isNotBlank(mapParam.get("ROWKEY"))) {//通过rowkey查询
                Map<String, String> map = hbaseDao.getByKey(HdrTableEnum.HDR_EMR_CONTENT.getCode(),
                        mapParam.get("ROWKEY"));
                list.add(map);
            }
            if (list.size() > 0) {
                rs.put(key, list.get(0).get(config[0]));
            }
        }
        return rs;
    }

	@Override
	public Map<String, String> getCommonNurseUrl(String patientId, String visitId, String visitType,
			Map<String, String> paramMap) {
		Map<String, String> rs = new HashMap<String, String>();
		boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
		if ("INPV".equals(visitType) && isDistinguish) {
			rs = getCommonUrl(patientId, visitId, "IN_NURSE", paramMap);
		} else if ("OUTPV".equals(visitType) && isDistinguish) {
			rs = getCommonUrl(patientId, visitId, "OUT_NURSE", paramMap);
		} else {
			rs = getCommonUrl(patientId, visitId, "NURSE", paramMap);
		}
		return rs;
	}

    @Override
    public Map<String, String> getPatientIdByBrid(String brid) {
        Map<String, String> result = new HashMap<String, String>();
        //通过brid查询solr获取pid,vid
        //查询条件
        //通过EID查询solr
        ResultVO docsMap = SolrQueryUtils.querySolr(SolrUtils.patInfoCollection, "BRID:" + brid, null,
                new String[]{"IN_PATIENT_ID,OUT_PATIENT_ID,VISIT_TYPE_CODE"}, 1000, 1, null, null);
        List<Map<String, String>> docList = docsMap.getResult();
        if (null != docList && docList.size() > 0) {
            for (Map<String, String> doc : docList) {
                String in_pid = doc.get("IN_PATIENT_ID");
                String out_pid = doc.get("OUT_PATIENT_ID");
                String pid = StringUtils.isNotBlank(in_pid) && (!"NULL".equals(in_pid)) ? in_pid : out_pid;
                result.put("PATIENT_ID", pid);
                result.put("VISIT_TYPE_CODE", (String) doc.get("VISIT_TYPE_CODE"));
                result.put("VISIT_ID", "1");
            }
        } else {
            result.put("status", "0");
        }
        result.put("url", Config.getCIV_RPC_DEFAULT_PAGE());
        return result;
    }
}
