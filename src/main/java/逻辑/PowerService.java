package 逻辑;

import com.goodwill.core.orm.Page;
import com.goodwill.core.orm.Page.Sort;
import com.goodwill.core.utils.PropertiesUtils;
import com.goodwill.hdr.civ.config.Config;
import com.goodwill.hdr.civ.config.ConfigCache;
import com.goodwill.hdr.civ.enums.HdrConstantEnum;
import com.goodwill.hdr.civ.utils.Utils;
import com.goodwill.hdr.civ.web.dao.PowerDao;
import com.goodwill.hdr.civ.web.entity.CommonConfig;
import com.goodwill.hdr.civ.web.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class PowerService implements PowerService {

	@Autowired
	PowerDao powerDao;

	@Autowired
	private PatientListService patientListService;
    @Autowired
    private OperService operService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CommonURLService commonURLService;

	private static final String CONFIG_FILE_NAME = "civ.properties";

	//判断是不是管理员用户
	@Override
	public Map<String, String> getCheckAdmin(String userCode) {
		// TODO Auto-generated method stub
		Map<String, String> map = new HashMap<String, String>();
		String admin = PropertiesUtils.getPropertyValue(CONFIG_FILE_NAME, "CIV_ADMIN");
		if (userCode.equals(admin))
			map.put("result", "1");
		else
			map.put("result", "0");
		return map;
	}

	//全局设置
	@Override
	public List<Map<String, String>> getSysConfig() {
		// TODO Auto-generated method stub
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		list = powerDao.getSysConfig();
		Utils.sortListByDate(list, "sort", Sort.ASC);
		return list;
	}

	@Override
	public boolean updateSysConfig(String configCode, String configValue) {
		// TODO Auto-generated method stub
		boolean is = powerDao.updateSysConfig(configCode, configValue);
		return is;
	}

	@Override
	public Map<String, String> getSysConfigByType(String configCode) {
		// TODO Auto-generated method stub
		Map<String, String> result = new HashMap<String, String>();
		Map<String, String> map = powerDao.getSysConfigByConfigCode(configCode);
		if ("1".equals(map.get("configValue")))
			result.put("result", "1");
		else
			result.put("result", "0");

		return result;
	}

	//用户权限设置

	@Override
	public Map<String, String> getPowerConfigByPage(String userCode) {
		// TODO Auto-generated method stub
		Map<String, String> rs = new HashMap<String, String>();
		if (userCode.equals(Config.getCiv_Admin())) {
			if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
				rs.put("Current", "1");
			} else {
				rs.put("Current", "0");
			}
			if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
				rs.put("Specialty", "1");
			} else {
				rs.put("Specialty", "0");
			}

			if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
				rs.put("TimeAxis", "1");
			} else {
				rs.put("TimeAxis", "0");
			}
			if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
				rs.put("SpecialtyTimeAxis", "1");
			} else {
				rs.put("SpecialtyTimeAxis", "0");
			}
			//新增体检视图
			if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
				rs.put("medicalView", "1");
			} else {
				rs.put("medicalView", "0");
			}
			rs.put("Visit", "1");
			rs.put("Category", "1");
			return rs;
		}
		String deptcode = powerDao.selectDeptByUser(userCode);
		Map<String, String> Current = new HashMap<String, String>();
		Current = powerDao.getPowerConfigByType(userCode, "Current");
		if (!StringUtils.isNotBlank(Current.get("itemCodes"))) {
			Current = powerDao.getPowerConfigByDeptAndType(deptcode, "Current");
		}
		if (!StringUtils.isNotBlank(Current.get("itemCodes"))) {
			rs.put("Current", "0");
		} else {
			rs.put("Current", "1");
		}
		Map<String, String> Specialty = new HashMap<String, String>();
		Specialty = powerDao.getPowerConfigByType(userCode, "Specialty");
		if (!StringUtils.isNotBlank(Specialty.get("itemCodes"))) {
			Specialty = powerDao.getPowerConfigByDeptAndType(deptcode, "Specialty");
		}
		if (!StringUtils.isNotBlank(Specialty.get("itemCodes"))) {
			rs.put("Specialty", "0");
		} else {
			rs.put("Specialty", "1");
		}
		Map<String, String> TimeAxis = new HashMap<String, String>();
		TimeAxis = powerDao.getPowerConfigByType(userCode, "TimeAxis");
		if (!StringUtils.isNotBlank(TimeAxis.get("itemCodes"))) {
			TimeAxis = powerDao.getPowerConfigByDeptAndType(deptcode, "TimeAxis");
		}
		if (!StringUtils.isNotBlank(TimeAxis.get("itemCodes"))) {
			rs.put("TimeAxis", "0");
		} else {
			rs.put("TimeAxis", "1");
		}
		Map<String, String> Visit = new HashMap<String, String>();
		Visit = powerDao.getPowerConfigByType(userCode, "Visit");
		if (!StringUtils.isNotBlank(Visit.get("itemCodes"))) {
			Visit = powerDao.getPowerConfigByDeptAndType(deptcode, "Visit");
		}
		if (!StringUtils.isNotBlank(Visit.get("itemCodes"))) {
			rs.put("Visit", "0");
		} else {
			rs.put("Visit", "1");
		}
		Map<String, String> Category = new HashMap<String, String>();
		Category = powerDao.getPowerConfigByType(userCode, "Category");
		if (!StringUtils.isNotBlank(Category.get("itemCodes"))) {
			Category = powerDao.getPowerConfigByDeptAndType(deptcode, "Category");
		}
		if (!StringUtils.isNotBlank(Category.get("itemCodes"))) {
			rs.put("Category", "0");
		} else {
			rs.put("Category", "1");
		}

		Map<String, String> SpecialtyTimeAxis = new HashMap<String, String>();
		SpecialtyTimeAxis = powerDao.getPowerConfigByType(userCode, "SpecialtyTimeAxis");
		if (!StringUtils.isNotBlank(SpecialtyTimeAxis.get("itemCodes"))) {
			SpecialtyTimeAxis = powerDao.getPowerConfigByDeptAndType(deptcode, "SpecialtyTimeAxis");
		}
		if (!StringUtils.isNotBlank(SpecialtyTimeAxis.get("itemCodes"))) {
			rs.put("SpecialtyTimeAxis", "0");
		} else {
			rs.put("SpecialtyTimeAxis", "1");
		}

		Map<String, String> medicalView = new HashMap<String, String>();
		medicalView = powerDao.getPowerConfigByType(userCode, "MedicalView");
		if (!StringUtils.isNotBlank(medicalView.get("itemCodes"))) {
			medicalView = powerDao.getPowerConfigByDeptAndType(deptcode, "MedicalView");
		}
		if (!StringUtils.isNotBlank(medicalView.get("itemCodes"))) {
			rs.put("medicalView", "0");
		} else {
			rs.put("medicalView", "1");
		}
		return rs;
	}

	@Override
	public List<Map<String, Object>> getPowerConfigByVisit(String userCode) {
		// TODO Auto-generated method stub
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		String admin = Config.getCiv_Admin();
		if (admin.equals(userCode)) {
			getAdminPowerByVisit(result);
			Utils.sortNumObjectList(result, "order", Sort.ASC);
			return result;
		}
		Map<String, String> map = new HashMap<String, String>();
		map = powerDao.getPowerConfigByType(userCode, "Visit");
		if (map.get("itemCodes") == null || "".equals(map.get("itemCodes"))) {
			String deptcode = powerDao.selectDeptByUser(userCode);
			map = powerDao.getPowerConfigByDeptAndType(deptcode, "Visit");
		}
		String[] values = map.get("itemCodes").toString().split("/");
		for (int i = 0; i < values.length; i++) {
			Map<String, Object> rs = new HashMap<String, Object>();
			String code = values[i];
			if (StringUtils.isNotBlank(values[i])) {
				if ("index_module".equals(code)) {
					rs.put("name", "首页");
					rs.put("id", "index_module");
					rs.put("order", "1");
					result.add(rs);
				} else if ("order_module".equals(code)) {
					rs.put("name", "医嘱");
					rs.put("id", "order_module");
					rs.put("order", "2");
					result.add(rs);
				} else if ("exam_module".equals(code)) {
					rs.put("name", "检验报告");
					rs.put("id", "exam_module");
					rs.put("order", "3");
					result.add(rs);
				} else if ("check_module".equals(code)) {
					rs.put("name", "检查报告");
					rs.put("id", "check_module");
					rs.put("order", "4");
					result.add(rs);
				} else if ("pathology_module".equals(code)) {
					rs.put("name", "病理报告");
					rs.put("id", "pathology_module");
					rs.put("order", "5");
					result.add(rs);
				} else if ("record_module".equals(code)) {
					rs.put("name", "病历文书");
					rs.put("id", "record_module");
					rs.put("linkType", "html");
					rs.put("order", "6");
					result.add(rs);
				} else if ("oper_module".equals(code)) {
					String moduleName = "手术记录";
					if(HdrConstantEnum.HOSPITAL_WHET.getCode().equals(ConfigCache.getCache("org_oid"))){
						moduleName = "手术过程";
					}
					rs.put("name", moduleName);
					rs.put("id", "oper_module");
                    if (StringUtils.isNotBlank(CommonConfig.getURL("OP"))) {
                        rs.put("linkType", CommonConfig.getLinkType("OP"));
                    } else {
                        rs.put("linkType", "civ");
                    }
					rs.put("order", "7");
					result.add(rs);
				} else if ("nurse_module".equals(code)) {
					rs.put("name", "护理记录");
					rs.put("id", "nurse_module");
					rs.put("linkType", "civ");
					boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
					if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("NURSE"));
					}
					if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("IN_NURSE"));
					}
					if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("OUT_NURSE"));
					}
					rs.put("order", "8");
					result.add(rs);
				} else if ("allergy_module".equals(code)) {
					rs.put("name", "过敏记录");
					rs.put("id", "allergy_module");
					rs.put("order", "9");
					result.add(rs);
				} else if ("ocl_module".equals(code)) {
					rs.put("name", "医嘱闭环");// 病历扫描件 医嘱闭环
					rs.put("id", "ocl_module");
					rs.put("linkType", CommonConfig.getLinkType("OCL"));
					rs.put("order", "10");
					result.add(rs);
				} else if ("blood_module".equals(code)) {
					rs.put("name", "临床用血");
					rs.put("id", "blood_module");
					rs.put("order", "11");
					result.add(rs);
				} else if ("web_emr_module".equals(code)) {
					rs.put("name", "WEB版电子病历");
					rs.put("id", "web_emr_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("WEBEMR"))) {
						rs.put("linkType", CommonConfig.getLinkType("WEBEMR"));
					} else {
						rs.put("linkType", "html");
					}
					rs.put("order", "12");
					result.add(rs);
				} else if ("fee_detail_module".equals(code)) {
					Map<String, Object> feeMap = new HashMap<String, Object>();
					feeMap.put("name", "费用明细");
					feeMap.put("id", "fee_detail_module");
					feeMap.put("order", "13");
					result.add(feeMap);
				}else if ("danger_nurse_module".equals(code)) {
					rs.put("name", "危重护理记录");
					rs.put("id", "danger_nurse_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("DANGERNURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("DANGERNURSE"));
					} else {
						rs.put("linkType", "html");
					}
					rs.put("order", "14");
					result.add(rs);
				}

			}
		}
		Utils.sortNumObjectList(result, "order", "asc");
		return result;
	}

	private void getAdminPowerByVisit(List<Map<String, Object>> result) {
		// TODO Auto-generated method stub
		String[] config = Config.getCIV_VISIT_VALUE().split("/");
		for (String module : config) {
			if ("index_module".equals(module)) {
				Map<String, Object> index = new HashMap<String, Object>();
				index.put("name", "首页");
				index.put("id", "index_module");
				index.put("order", "1");
				result.add(index);
			}
			if ("order_module".equals(module)) {
				Map<String, Object> order = new HashMap<String, Object>();
				order.put("name", "医嘱");
				order.put("id", "order_module");
				order.put("order", "2");
				result.add(order);
			}
			if ("exam_module".equals(module)) {
				Map<String, Object> exam = new HashMap<String, Object>();
				exam.put("name", "检验报告");
				exam.put("id", "exam_module");
				exam.put("order", "3");
				result.add(exam);
			}
			if ("check_module".equals(module)) {
				Map<String, Object> check = new HashMap<String, Object>();
				check.put("name", "检查报告");
				check.put("id", "check_module");
				check.put("order", "4");
				result.add(check);
			}
			if ("pathology_module".equals(module)) {
				Map<String, Object> pathology = new HashMap<String, Object>();
				pathology.put("name", "病理报告");
				pathology.put("id", "pathology_module");
				pathology.put("order", "5");
				result.add(pathology);
			}
			if ("record_module".equals(module)) {
				Map<String, Object> record = new HashMap<String, Object>();
				record.put("name", "病历文书");
				record.put("id", "record_module");
				if (StringUtils.isNotBlank(CommonConfig.getURL("RM"))) {
					record.put("linkType", CommonConfig.getLinkType("RM"));
				} else {
					record.put("linkType", "html");
				}
				record.put("order", "6");
				result.add(record);
			}
			if ("oper_module".equals(module)) {
				Map<String, Object> oper = new HashMap<String, Object>();
				String moduleName = "手术记录";
				if(HdrConstantEnum.HOSPITAL_WHET.getCode().equals(ConfigCache.getCache("org_oid"))){
					moduleName = "手术过程";
				}
				oper.put("name", moduleName);
				oper.put("id", "oper_module");
                if (StringUtils.isNotBlank(CommonConfig.getURL("OP"))) {
                    oper.put("linkType", CommonConfig.getLinkType("OP"));
                } else {
                    oper.put("linkType", "civ");
                }
				oper.put("order", "7");
				result.add(oper);
			}
			if ("nurse_module".equals(module)) {
				Map<String, Object> nurse = new HashMap<String, Object>();
				nurse.put("name", "护理记录");
				nurse.put("id", "nurse_module");
				nurse.put("linkType", "civ");
				boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
				if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE"))) {
					nurse.put("linkType", CommonConfig.getLinkType("NURSE"));
				}
				if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE"))) {
					nurse.put("linkType", CommonConfig.getLinkType("IN_NURSE"));
				}
				if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE"))) {
					nurse.put("linkType", CommonConfig.getLinkType("OUT_NURSE"));
				}
				nurse.put("order", "8");
				result.add(nurse);
			}
			if ("allergy_module".equals(module)) {
				Map<String, Object> allergy = new HashMap<String, Object>();
				allergy.put("name", "过敏记录");
				allergy.put("id", "allergy_module");
				allergy.put("order", "9");
				result.add(allergy);
			}
			if ("ocl_module".equals(module)) {
				Map<String, Object> oclMap = new HashMap<String, Object>();
				oclMap.put("name", "医嘱闭环");// 病历扫描件 医嘱闭环
				oclMap.put("id", "ocl_module");
				oclMap.put("order", "10");
				result.add(oclMap);
			}
			if ("blood_module".equals(module)) {
				Map<String, Object> bloodMap = new HashMap<String, Object>();
				bloodMap.put("name", "临床用血");
				bloodMap.put("id", "blood_module");
				bloodMap.put("order", "11");
				result.add(bloodMap);
			}
			if ("fee_detail_module".equals(module)) {
				Map<String, Object> feeMap = new HashMap<String, Object>();
				feeMap.put("name", "费用明细");
				feeMap.put("id", "fee_detail_module");
				feeMap.put("order", "12");
				result.add(feeMap);
			}
			if ("web_emr_module".equals(module)) {
				Map<String, Object> webEmrMap = new HashMap<String, Object>();
				webEmrMap.put("name", "WEB版电子病历");
				webEmrMap.put("id", "web_emr_module");
				if (StringUtils.isNotBlank(CommonConfig.getURL("WEBEMR"))) {
					webEmrMap.put("linkType", CommonConfig.getLinkType("WEBEMR"));
				} else {
					webEmrMap.put("linkType", "html");
				}
				webEmrMap.put("order", "13");
				result.add(webEmrMap);
			}
		}
	}

	@Override
	public List<Map<String, Object>> getPowerConfigByCategory(String userCode) {
		// TODO Auto-generated method stub
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		String admin = Config.getCiv_Admin();
		if (admin.equals(userCode)) {
			getAdminPowerByCategory(result);
			Utils.sortNumObjectList(result, "order", Sort.ASC);
			return result;
		}
		Map<String, String> map = new HashMap<String, String>();
		map = powerDao.getPowerConfigByType(userCode, "Category");
		if (map.get("itemCodes") == null || "".equals(map.get("itemCodes"))) {
			String deptcode = powerDao.selectDeptByUser(userCode);
			map = powerDao.getPowerConfigByDeptAndType(deptcode, "Category");
		}
		String[] values = map.get("itemCodes").toString().split("/");
		for (int i = 0; i < values.length; i++) {
			Map<String, Object> rs = new HashMap<String, Object>();
			String code = values[i];
			if (StringUtils.isNotBlank(values[i])) {
				if ("check_module".equals(code)) {
					rs.put("name", "检查报告");
					rs.put("id", "check_module");
					rs.put("order", "1");
					result.add(rs);
				} else if ("exam_module".equals(code)) {
					rs.put("name", "检验报告");
					rs.put("id", "exam_module");
					rs.put("order", "2");
					result.add(rs);
				} else if ("oper_module".equals(code)) {
					String moduleName = "手术记录";
					if(HdrConstantEnum.HOSPITAL_WHET.getCode().equals(ConfigCache.getCache("org_oid"))){
						moduleName = "手术过程";
					}
					rs.put("name", moduleName);
					rs.put("id", "oper_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("OP"))) {
						rs.put("linkType", CommonConfig.getLinkType("OP"));
					} else {
						rs.put("linkType", "civ");
					}
					rs.put("order", "3");
					result.add(rs);
				} else if ("main_diag_module".equals(code)) {
					rs.put("name", "主要疾病诊断");
					rs.put("id", "main_diag_module");
					rs.put("order", "4");
					result.add(rs);
				} else if ("pathology_module".equals(code)) {
					rs.put("name", "病理报告");
					rs.put("id", "pathology_module");
					rs.put("order", "5");
					result.add(rs);
				} else if ("record_module".equals(code)) {
					rs.put("name", "病历文书");
					rs.put("id", "record_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("RM"))) {
						rs.put("linkType", CommonConfig.getLinkType("RM"));
					} else {
						rs.put("linkType", "html");
					}
					rs.put("order", "6");
					result.add(rs);
				} else if ("nurse_module".equals(code)) {
					rs.put("name", "护理记录");
					rs.put("id", "nurse_module");
					rs.put("linkType", "civ");
					boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
					if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("NURSE"));
					}
					if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("IN_NURSE"));
					}
					if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("OUT_NURSE"));
					}
					rs.put("order", "7");
					result.add(rs);
				} else if ("durg_orally_module".equals(code)) {
					rs.put("name", "口服药品");
					rs.put("id", "durg_orally_module");
					rs.put("order", "8");
					result.add(rs);
				} else if ("durg_vein_module".equals(code)) {
					rs.put("name", "静脉药品");
					rs.put("id", "durg_vein_module");
					rs.put("order", "9");
					result.add(rs);
				} else if ("durg_qt_module".equals(code)) {
					rs.put("name", "其他药品");
					rs.put("id", "durg_qt_module");
					rs.put("order", "10");
					result.add(rs);
				} else if ("history_module".equals(code)) {
					rs.put("name", "院前用药");
					rs.put("id", "history_module");
					rs.put("order", "11");
					result.add(rs);
				} else if ("dialysis_module".equals(code)) {
					rs.put("name", "血液透析");
					rs.put("id", "dialysis_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("HD"))) {
						rs.put("linkType", CommonConfig.getLinkType("HD"));
					} else {
						rs.put("linkType", "html");
					}
					rs.put("order", "12");
					result.add(rs);
				} else if ("web_emr_module".equals(code)) {
					rs.put("name", "WEB版电子病历");
					rs.put("id", "web_emr_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("WEBEMR"))) {
						rs.put("linkType", CommonConfig.getLinkType("WEBEMR"));
					} else {
						rs.put("linkType", "html");
					}
					rs.put("order", "13");
					result.add(rs);
				}else if ("fee_detail_module".equals(code)) {
					rs.put("name", "费用明细");
					rs.put("id", "fee_detail_module");
					rs.put("linkType", "html");
					rs.put("order", "94");
					result.add(rs);
				}else if ("danger_nurse_module".equals(code)) {
					rs.put("name", "危重护理记录");
					rs.put("id", "danger_nurse_module");
					if (StringUtils.isNotBlank(CommonConfig.getURL("DANGERNURSE"))) {
						rs.put("linkType", CommonConfig.getLinkType("DANGERNURSE"));
					} else {
						rs.put("linkType", "html");
					}
					rs.put("order", "95");
					result.add(rs);
				}
			}
		}
		Utils.sortNumObjectList(result, "order", Sort.ASC);
		return result;
	}

	private void getAdminPowerByCategory(List<Map<String, Object>> result) {
		// TODO Auto-generated method stub

		String[] config = Config.getCIV_CATEGORY_VALUE().split("/");
		for (int i = 0; i < config.length; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			String key = config[i];
			if ("check_module".equals(key)) {
				map.put("name", "检查报告");
				map.put("id", "check_module");
				result.add(map);
			} else if ("exam_module".equals(key)) {
				map.put("name", "检验报告");
				map.put("id", "exam_module");
				result.add(map);
			} else if ("oper_module".equals(key)) {
				String moduleName = "手术记录";
				if(HdrConstantEnum.HOSPITAL_WHET.getCode().equals(ConfigCache.getCache("org_oid"))){
					moduleName = "手术过程";
				}
				map.put("name", moduleName);
				map.put("id", "oper_module");
				if (StringUtils.isNotBlank(CommonConfig.getURL("OP"))) {
					map.put("linkType", CommonConfig.getLinkType("OP"));
				} else {
					map.put("linkType", "civ");
				}
				result.add(map);
			} else if ("main_diag_module".equals(key)) {
				map.put("name", "主要疾病诊断");
				map.put("id", "main_diag_module");
				result.add(map);
			} else if ("pathology_module".equals(key)) {
				map.put("name", "病理报告");
				map.put("id", "pathology_module");
				result.add(map);
			} else if ("record_module".equals(key)) {
				map.put("name", "病历文书");
				map.put("id", "record_module");
				String linkType = "html";
				if (StringUtils.isNotBlank(CommonConfig.getURL("RM"))) {
					linkType = CommonConfig.getLinkType("RM");
				} else {
					map.put("linkType", "html");
				}
				map.put("linkType", linkType);
				result.add(map);
			} else if ("nurse_module".equals(key)) {
				map.put("name", "护理记录");
				map.put("id", "nurse_module");
				map.put("linkType", "civ");
				boolean isDistinguish = Config.getCIV_NURSE_URL_OUT_OR_IN();
				if (StringUtils.isNotBlank(CommonConfig.getURL("NURSE"))) {
					map.put("linkType", CommonConfig.getLinkType("NURSE"));
				}
				if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("IN_NURSE"))) {
					map.put("linkType", CommonConfig.getLinkType("IN_NURSE"));
				}
				if (isDistinguish && StringUtils.isNotBlank(CommonConfig.getURL("OUT_NURSE"))) {
					map.put("linkType", CommonConfig.getLinkType("OUT_NURSE"));
				}
				result.add(map);
			} else if ("durg_orally_module".equals(key)) {
				map.put("name", "口服药品");
				map.put("id", "durg_orally_module");
				result.add(map);
			} else if ("durg_vein_module".equals(key)) {
				map.put("name", "静脉药品");
				map.put("id", "durg_vein_module");
				result.add(map);
			} else if ("durg_qt_module".equals(key)) {
				map.put("name", "其他药品");
				map.put("id", "durg_qt_module");
				result.add(map);
			} else if ("history_module".equals(key)) {
				map.put("name", "院前用药");
				map.put("id", "history_module");
				result.add(map);
			} else if ("dialysis_module".equals(key)) {
				map.put("name", "血液透析");
				map.put("id", "dialysis_module");
				if (StringUtils.isNotBlank(CommonConfig.getURL("HD"))) {
					map.put("linkType", CommonConfig.getLinkType("HD"));
				} else {
					map.put("linkType", "html");
				}
				map.put("order", "92");
				result.add(map);
			} else if ("web_emr_module".equals(key)) {
				map.put("name", "WEB版电子病历");
				map.put("id", "web_emr_module");
				if (StringUtils.isNotBlank(CommonConfig.getURL("WEBEMR"))) {
					map.put("linkType", CommonConfig.getLinkType("WEBEMR"));
				} else {
					map.put("linkType", "html");
				}
				map.put("order", "93");
				result.add(map);
			} else if ("fee_detail_module".equals(key)) {
				map.put("name", "费用明细");
				map.put("id", "fee_detail_module");
				map.put("linkType", "html");
				map.put("order", "94");
				result.add(map);
			}
		}
	}

	@Override
	public Map<String, Object> getPowerConfigByEMR(String userCode) {
		// TODO Auto-generated method stub
		Map<String, Object> result = new HashMap<String, Object>();
		String admin = Config.getCiv_Admin();
		if (admin.equals(userCode)) {
			result.put("isAll", "true");
			result.put("power", "all");
			return result;
		}
		Map<String, String> map = new HashMap<String, String>();
		map = powerDao.getPowerConfigByType(userCode, "Mr");
		if (StringUtils.isBlank(map.get("itemCodes"))) {
			String deptcode = powerDao.selectDeptByUser(userCode);
			map = powerDao.getPowerConfigByDeptAndType(deptcode, "Mr");
		}
		String[] values = map.get("itemCodes").toString().split("/");
		List<String> tmp = new ArrayList<String>();
		for (int i = 0; i < values.length; i++) {
			if (StringUtils.isNotBlank(values[i])) {
				tmp.add(values[i]);
			}
		}
		//如果权限中包含all，则返回true，不包含all，返回false和相应权限
		if (tmp.contains("all")) {
			result.put("isAll", "true");
			result.put("power", "all");
		} else {
			result.put("isAll", "false");
			result.put("power", tmp);
		}
		return result;
	}

	@Override
	public Map<String, Object> getPowerConfigByExam(String userCode) {
		// TODO Auto-generated method stub
		Map<String, Object> result = new HashMap<String, Object>();
		String admin = Config.getCiv_Admin();
		if (admin.equals(userCode)) {
			result.put("isAll", "true");
			result.put("power", "all");
			return result;
		}
		Map<String, String> map = new HashMap<String, String>();
		map = powerDao.getPowerConfigByType(userCode, "Exam");
		if (StringUtils.isBlank(map.get("itemCodes"))) {
			String deptcode = powerDao.selectDeptByUser(userCode);
			map = powerDao.getPowerConfigByDeptAndType(deptcode, "Exam");
		}
		String[] values = map.get("itemCodes").toString().split("/");
		List<String> tmp = new ArrayList<String>();
		for (int i = 0; i < values.length; i++) {
			if (StringUtils.isNotBlank(values[i])) {
				tmp.add(values[i]);
			}
		}
		//如果权限中包含all，则返回true，不包含all，返回false和相应权限
		if (tmp.contains("all")) {
			result.put("isAll", "true");
			result.put("power", "all");
		} else {
			result.put("isAll", "false");
			result.put("power", tmp);
		}
		return result;
	}

	@Override
	public Map<String, Object> getPowerConfigByPathology(String userCode) {
		// TODO Auto-generated method stub
		Map<String, Object> result = new HashMap<String, Object>();
		String admin = Config.getCiv_Admin();
		if (admin.equals(userCode)) {
			result.put("isAll", "true");
			result.put("power", "all");
			return result;
		}
		Map<String, String> map = new HashMap<String, String>();
		map = powerDao.getPowerConfigByType(userCode, "Pathology");
		if (StringUtils.isBlank(map.get("itemCodes"))) {
			String deptcode = powerDao.selectDeptByUser(userCode);
			map = powerDao.getPowerConfigByDeptAndType(deptcode, "Pathology");
		}
		String[] values = map.get("itemCodes").toString().split("/");
		List<String> tmp = new ArrayList<String>();
		for (int i = 0; i < values.length; i++) {
			if (StringUtils.isNotBlank(values[i])) {
				tmp.add(values[i]);
			}
		}
		//如果权限中包含all，则返回true，不包含all，返回false和相应权限
		if (tmp.contains("all")) {
			result.put("isAll", "true");
			result.put("power", "all");
		} else {
			result.put("isAll", "false");
			result.put("power", tmp);
		}
		return result;
	}

	@Override
	public Map<String, String> updatePowerConfigByUser(String userCodes, String Current, String Specialty,
			String TimeAxis, String Visit, String Category, String Mr, String Exam, String Pathology,
			String specialtyTimeAxis, String medicalView) {
		// TODO Auto-generated method stub
		String result = "";
		Map<String, String> map = new HashMap<String, String>();
		String[] users = userCodes.split("/");
		for (int i = 0; i < users.length; i++) {
			List<Map<String, String>> configs = new ArrayList<Map<String, String>>();
			configs = powerDao.getPowerConfig(users[i], true);
			if (configs.size() == 0) {
				String deptCode = powerDao.selectDeptByUser(users[i]);
//				insertPowerConfigByUser(users[i], deptCode);
				//上面一行注释掉了，原来第一次设置权限的时候是从科室里面取权限，不是页面设置的权限，导致第一次配置的权限不生效
				//原本的设计是第一次是初始化的
				DateFormat bf = new SimpleDateFormat("yyyy-MM-dd E a HH:mm:ss");
				String date = bf.format(new Date());
				if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
					if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Current", Current,date))
						result = result + "当前视图设置权限失败。";
				}
				if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
					if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Specialty", Specialty,date))
						result = result + "专科视图设置权限失败。";
				}
				if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
						if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "TimeAxis", StringUtils.isBlank(TimeAxis) ? "":TimeAxis,date))
						result = result + "时间轴视图设置权限失败。";
				}
				if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
						if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "SpecialtyTimeAxis", StringUtils.isBlank(specialtyTimeAxis) ? "":specialtyTimeAxis,date))
						result = result + "专科视图时间轴设置权限失败。";
				}
				if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
						if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "MedicalView", StringUtils.isBlank(medicalView) ? "":medicalView,date))
							result = result + "体检视图设置权限失败。";
				}
				if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Visit", StringUtils.isBlank(Visit) ? "" : Visit, date))
					result = result + "就诊视图设置权限失败。";
				if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Category", StringUtils.isBlank(Category) ? "" : Category, date))
					result = result + "分类视图设置权限失败。";
				if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Mr", StringUtils.isBlank(Mr) ? "" : Mr, date))
					result = result + "病历文书设置权限失败。";
				if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Exam", StringUtils.isBlank(Exam) ? "" : Exam, date))
					result = result + "检查报告设置权限失败。";
				if (!powerDao.insertPowerConfigByUser(users[i], deptCode, "Pathology", StringUtils.isBlank(Pathology) ? "" : Pathology, date))
					result = result + "病理报告设置权限失败。";

			} else {
				if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
					if (!updatePowerConfigByType(userCodes, "Current", Current, true))
						result = result + "当前视图设置权限失败。";
				}
				if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
					if (!updatePowerConfigByType(userCodes, "Specialty", Specialty, true))
						result = result + "专科视图设置权限失败。";
				}
				if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
					if (!updatePowerConfigByType(userCodes, "TimeAxis", StringUtils.isBlank(TimeAxis) ? "" : TimeAxis,
							true))
						result = result + "时间轴视图设置权限失败。";
				}
				if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
					if (!updatePowerConfigByType(userCodes, "SpecialtyTimeAxis",
							StringUtils.isBlank(specialtyTimeAxis) ? "" : specialtyTimeAxis, true))
						result = result + "专科视图时间轴设置权限失败。";
				}
				if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
					if (!updatePowerConfigByType(userCodes, "MedicalView",
							StringUtils.isBlank(medicalView) ? "" : medicalView, true))
						result = result + "体检视图设置权限失败。";
				}
				if (!updatePowerConfigByType(userCodes, "Visit", Visit, true))
					result = result + "就诊视图设置权限失败。";
				if (!updatePowerConfigByType(userCodes, "Category", Category, true))
					result = result + "分类视图设置权限失败。";
				if (!updatePowerConfigByType(userCodes, "Mr", Mr, true))
					result = result + "病历文书设置权限失败。";
				if (!updatePowerConfigByType(userCodes, "Exam", Exam, true))
					result = result + "检查报告设置权限失败。";
				if (!updatePowerConfigByType(userCodes, "Pathology", Pathology, true))
					result = result + "病理报告设置权限失败。";
			}
		}
		if (!"".equals(result)) {
			map.put("result", "0");
			map.put("msg", result);
		} else {
			map.put("result", "1");
		}
		return map;
	}

	@Override
	public Map<String, String> updatePowerConfigByDept(String deptCodes, String Current, String Specialty,
			String TimeAxis, String Visit, String Category, String Mr, String Exam, String Pathology,
			String specialtyTimeAxis, String medicalView) {
		// TODO Auto-generated method stub
		Map<String, String> map = new HashMap<String, String>();
		String result = "";
		if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
			if (!updatePowerConfigByType(deptCodes, "Current", StringUtils.isBlank(Current) ? "" : Current, false))
				result = result + "当前视图设置权限失败。";
		}
		if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
			if (!updatePowerConfigByType(deptCodes, "Specialty", Specialty, false))
				result = result + "专科视图设置权限失败。";
		}
		if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
			if (!updatePowerConfigByType(deptCodes, "TimeAxis", StringUtils.isBlank(TimeAxis) ? "" : TimeAxis, false))
				result = result + "时间轴视图设置权限失败。";
		}
		if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
			if (!updatePowerConfigByType(deptCodes, "SpecialtyTimeAxis",
					StringUtils.isBlank(specialtyTimeAxis) ? "" : specialtyTimeAxis, false))
				result = result + "专科视图时间轴设置权限失败。";
		}
		if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
			if (!updatePowerConfigByType(deptCodes, "MedicalView", StringUtils.isBlank(medicalView) ? "" : medicalView,
					false))
				result = result + "体检视图设置权限失败。";
		}
		if (!updatePowerConfigByType(deptCodes, "Visit", Visit, false))
			result = result + "就诊视图设置权限失败。";
		if (!updatePowerConfigByType(deptCodes, "Category", Category, false))
			result = result + "分类视图设置权限失败。";
		if (!updatePowerConfigByType(deptCodes, "Mr", Mr, false))
			result = result + "病历文书设置权限失败。";
		if (!updatePowerConfigByType(deptCodes, "Exam", Exam, false))
			result = result + "检查报告设置权限失败。";
		if (!updatePowerConfigByType(deptCodes, "Pathology", Pathology, false))
			result = result + "病理报告设置权限失败。";
		if (!"".equals(result)) {
			map.put("result", "0");
			map.put("msg", result);
		} else {
			map.put("result", "1");
		}
		return map;
	}

	public boolean updatePowerConfigByType(String codes, String type, String value, boolean isUser) {
		// TODO Auto-generated method stub
		String[] users = codes.split("/");
		boolean is = powerDao.updatePowerConfigByType(users, type, value, isUser);
		return is;
	}

	@Override
	public Map<String, Object> getPowerConfigByUser(String userCode) {
		// TODO Auto-generated method stub
		Map<String, Object> rs = new HashMap<String, Object>();
		List<Map<String, String>> configs = new ArrayList<Map<String, String>>();
		String admin = Config.getCiv_Admin();
		if (admin.equals(userCode)) {
			getAdminPower(configs, userCode, false);
		} else {
			configs = powerDao.getPowerConfig(userCode, true);
		}
		if (configs.size() == 0) {
			String deptCode = powerDao.selectDeptByUser(userCode);
			rs = getPowerConfigByDept(deptCode);
			return rs;
		}
		List<Map<String, Object>> visitSets = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> contentSets = new ArrayList<Map<String, Object>>();
		for (Map<String, String> config : configs) {
			Map<String, Object> map = new HashMap<String, Object>();
			if ("Current".equals(config.get("type"))) {
				map.put("name", "当前视图");
				map.put("id", "Current");
				map.put("text", "");
				map.put("order", "1");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "current");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("Specialty".equals(config.get("type"))) {
				map.put("name", "专科视图");
				map.put("id", "Specialty");
				map.put("text", "");
				map.put("order", "2");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "specialty");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("TimeAxis".equals(config.get("type"))) {
				map.put("name", "时间轴视图");
				map.put("id", "TimeAxis");
				map.put("text", "");
				map.put("order", "3");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "timeaxis");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("Visit".equals(config.get("type"))) {
				map.put("name", "就诊视图");
				map.put("id", "Visit");
				map.put("text", "选择该页面的全部子页面");
				map.put("order", "4");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Visit();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				visitSets.add(map);
			} else if ("Category".equals(config.get("type"))) {
				map.put("name", "分类视图");
				map.put("id", "Category");
				map.put("text", "选择该页面的全部子页面");
				map.put("order", "5");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Category();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				visitSets.add(map);
			} else if ("Mr".equals(config.get("type"))) {
				map.put("name", "病历文书");
				map.put("id", "Mr");
				map.put("text", "选择全部文书类型");
				map.put("order", "6");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Emr_Types();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				contentSets.add(map);
			} else if ("Exam".equals(config.get("type"))) {
				map.put("name", "检查报告");
				map.put("id", "Exam");
				map.put("text", "选择全部报告类型");
				map.put("order", "7");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Exam_Types();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				contentSets.add(map);
			} else if ("SpecialtyTimeAxis".equals(config.get("type"))) {
				map.put("name", "专科视图时间轴");
				map.put("id", "SpecialtyTimeAxis");
				map.put("text", "");
				map.put("order", "8");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "specialtytimeaxis");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("MedicalView".equals(config.get("type"))) {
				map.put("name", "体检视图");
				map.put("id", "MedicalView");
				map.put("text", "选择");
				map.put("order", "9");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "medicalview");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("Pathology".equals(config.get("type"))) {
				map.put("name", "病理报告");
				map.put("id", "Pathology");
				map.put("text", "选择全部报告类型");
				map.put("order", "10");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCIV_PATHOLOGY_TYPE();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				contentSets.add(map);
			}
		}
		Utils.sortListByDate(visitSets, "order", Sort.ASC);
		Utils.sortListByDate(contentSets, "order", Sort.ASC);
		rs.put("visitSet", visitSets);
		rs.put("contentSet", contentSets);

		return rs;
	}

	@Override
	public Map<String, Object> getPowerConfigByDept(String deptCode) {
		// TODO Auto-generated method stub
		Map<String, Object> rs = new HashMap<String, Object>();
		List<Map<String, String>> configs = new ArrayList<Map<String, String>>();
		configs = powerDao.getPowerConfig(deptCode, false);
		if (configs.size() == 0) {
			insertPowerConfigByDept(deptCode);
			getAdminPower(configs, deptCode, true);
		}
		List<Map<String, Object>> visitSets = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> contentSets = new ArrayList<Map<String, Object>>();
		for (Map<String, String> config : configs) {
			Map<String, Object> map = new HashMap<String, Object>();
			if ("Current".equals(config.get("type"))) {
				map.put("name", "当前视图");
				map.put("id", "Current");
				map.put("text", "");
				map.put("order", "1");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "current");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("Specialty".equals(config.get("type"))) {
				map.put("name", "专科视图");
				map.put("id", "Specialty");
				map.put("text", "");
				map.put("order", "2");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "specialty");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("TimeAxis".equals(config.get("type"))) {
				map.put("name", "时间轴视图");
				map.put("id", "TimeAxis");
				map.put("text", "");
				map.put("order", "3");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "timeaxis");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("Visit".equals(config.get("type"))) {
				map.put("name", "就诊视图");
				map.put("id", "Visit");
				map.put("text", "选择该页面的全部子页面");
				map.put("order", "4");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Visit();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				visitSets.add(map);
			} else if ("Category".equals(config.get("type"))) {
				map.put("name", "分类视图");
				map.put("id", "Category");
				map.put("text", "选择该页面的全部子页面");
				map.put("order", "5");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Category();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				visitSets.add(map);
			} else if ("Mr".equals(config.get("type"))) {
				map.put("name", "病历文书");
				map.put("id", "Mr");
				map.put("text", "选择全部文书类型");
				map.put("order", "6");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Emr_Types();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				contentSets.add(map);
			} else if ("Exam".equals(config.get("type"))) {
				map.put("name", "检查报告");
				map.put("id", "Exam");
				map.put("text", "选择全部报告类型");
				map.put("order", "7");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCiv_Exam_Types();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				contentSets.add(map);
			} else if ("SpecialtyTimeAxis".equals(config.get("type"))) {
				map.put("name", "专科视图时间轴");
				map.put("id", "SpecialtyTimeAxis");
				map.put("text", "");
				map.put("order", "8");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "specialtytimeaxis");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("MedicalView".equals(config.get("type"))) {
				map.put("name", "体检视图");
				map.put("id", "MedicalView");
				map.put("text", "选择");
				map.put("order", "9");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> map2 = new HashMap<String, String>();
				map2.put("name", "选择");
				if (config.get("itemCodes") == null || "".equals(config.get("itemCodes"))) {
					map2.put("value", "0");
				} else {
					map2.put("value", "1");
				}
				map2.put("code", "medicalview");
				list.add(map2);
				map.put("list", list);
				visitSets.add(map);
			} else if ("Pathology".equals(config.get("type"))) {
				map.put("name", "病理报告");
				map.put("id", "Pathology");
				map.put("text", "选择全部报告类型");
				map.put("order", "10");
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				List<Map<String, String>> types = Config.getCIV_PATHOLOGY_TYPE();
				getTypeList(types, list, config.get("itemCodes"));
				map.put("list", list);
				contentSets.add(map);
			}
		}
		Utils.sortListByDate(visitSets, "order", Sort.ASC);
		Utils.sortListByDate(contentSets, "order", Sort.ASC);
		rs.put("visitSet", visitSets);
		rs.put("contentSet", contentSets);

		return rs;
	}

	private void insertPowerConfigByUser(String userCode, String deptCode) {
		// TODO Auto-generated method stub
		List<Map<String, String>> configs = new ArrayList<Map<String, String>>();
		configs = powerDao.getPowerConfig(deptCode, false);
		DateFormat bf = new SimpleDateFormat("yyyy-MM-dd E a HH:mm:ss");
		String date = bf.format(new Date());
		for (Map<String, String> map : configs) {
			powerDao.insertPowerConfigByUser(userCode, deptCode, map.get("type"), map.get("itemCodes"), date);
		}
	}

	private void insertPowerConfigByDept(String deptCode) {
		// TODO Auto-generated method stub
		DateFormat bf = new SimpleDateFormat("yyyy-MM-dd E a HH:mm:ss");
		String date = bf.format(new Date());
		if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "Current", Config.getCIV_CURRENT_VALUE(), date);
		}
		if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "Specialty", Config.getCIV_SPECIALTY_VALUE(), date);
		}
		if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "TimeAxis", Config.getCIV_TIMEAXIS_VALUE(), date);
		}
		if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "SpecialtyTimeAxis", Config.getCIV_SPECIALTYTIMEAXIS_VALUE(),
					date);
		}
		if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "MedicalView", Config.getCIV_MEDICAL_VALUE(), date);
		}
		powerDao.insertPowerConfigByDept(deptCode, "Visit", Config.getCIV_VISIT_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Category", Config.getCIV_CATEGORY_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Mr", Config.getCIV_EMR_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Exam", Config.getCIV_EXAM_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Pathology", Config.getCIV_PATHOLOGY_VALUE(), date);
	}

	private void getAdminPower(List<Map<String, String>> configs, String code, boolean isDept) {
		// TODO Auto-generated method stub
		String userCode;
		String deptCode;
		if (isDept) {
			userCode = "";
			deptCode = code;
		} else {
			userCode = code;
			deptCode = "";
		}

		if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
			Map<String, String> Current = new HashMap<String, String>();
			Current.put("itemCodes", Config.getCIV_CURRENT_VALUE());
			Current.put("userCode", userCode);
			Current.put("deptCode", deptCode);
			Current.put("type", "Current");
			configs.add(Current);
		}

		if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
			Map<String, String> Specialty = new HashMap<String, String>();
			Specialty.put("itemCodes", Config.getCIV_SPECIALTY_VALUE());
			Specialty.put("userCode", userCode);
			Specialty.put("deptCode", deptCode);
			Specialty.put("type", "Specialty");
			configs.add(Specialty);
		}

		if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
			Map<String, String> TimeAxis = new HashMap<String, String>();
			TimeAxis.put("itemCodes", Config.getCIV_SPECIALTY_VALUE());
			TimeAxis.put("userCode", userCode);
			TimeAxis.put("deptCode", deptCode);
			TimeAxis.put("type", "TimeAxis");
			configs.add(TimeAxis);
		}
		if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
			Map<String, String> specialtyTimeaxis = new HashMap<String, String>();
			specialtyTimeaxis.put("itemCodes", Config.getCIV_SPECIALTYTIMEAXIS_VALUE());
			specialtyTimeaxis.put("userCode", userCode);
			specialtyTimeaxis.put("deptCode", deptCode);
			specialtyTimeaxis.put("type", "SpecialtyTimeAxis");
			configs.add(specialtyTimeaxis);
		}

		Map<String, String> Category = new HashMap<String, String>();
		Category.put("itemCodes", Config.getCIV_CATEGORY_VALUE());
		Category.put("userCode", userCode);
		Category.put("deptCode", deptCode);
		Category.put("type", "Category");
		configs.add(Category);

		Map<String, String> Mr = new HashMap<String, String>();
		Mr.put("itemCodes", Config.getCIV_EMR_VALUE());
		Mr.put("userCode", userCode);
		Mr.put("deptCode", deptCode);
		Mr.put("type", "Mr");
		configs.add(Mr);

		Map<String, String> Exam = new HashMap<String, String>();
		Exam.put("itemCodes", Config.getCIV_EXAM_VALUE());
		Exam.put("userCode", userCode);
		Exam.put("deptCode", deptCode);
		Exam.put("type", "Exam");
		configs.add(Exam);

		Map<String, String> Visit = new HashMap<String, String>();
		Visit.put("itemCodes", Config.getCIV_VISIT_VALUE());
		Visit.put("userCode", userCode);
		Visit.put("deptCode", deptCode);
		Visit.put("type", "Visit");
		configs.add(Visit);

		//新增体检视图权限
		if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
			Map<String, String> medicalView = new HashMap<String, String>();
			medicalView.put("itemCodes", "MedicalView");
			medicalView.put("userCode", userCode);
			medicalView.put("deptCode", deptCode);
			medicalView.put("type", "MedicalView");
			configs.add(medicalView);
		}

		Map<String, String> Pathology = new HashMap<String, String>();
		Pathology.put("itemCodes", Config.getCIV_PATHOLOGY_VALUE());
		Pathology.put("userCode", userCode);
		Pathology.put("deptCode", deptCode);
		Pathology.put("type", "Pathology");
		configs.add(Pathology);

	}

	/**
	 * 根据配置文件和数据对比查询权限
	 *
	 * @param types
	 * @param list
	 * @param
	 */
	private void getTypeList(List<Map<String, String>> types, List<Map<String, String>> list, String itemCodes) {
		// TODO Auto-generated method stub
//		itemCodes = itemCodes.replaceAll("\\\\","\\\\\\\\");
		for (int i = 0; i < types.size(); i++) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("name", types.get(i).get("name"));
			map.put("code", types.get(i).get("code"));
			if (itemCodes.indexOf(types.get(i).get("code")) != -1) {
				map.put("value", "1");
			} else {
				map.put("value", "0");
			}
			list.add(map);
		}
	}

	//用户列表
	@Override
	public Map<String, Object> getUserList(String userName, String deptCode, int pageNo, int pageSize) {
		// TODO Auto-generated method stub
		Map<String, Object> rs = new HashMap<String, Object>();
		if (pageNo == 0) {
			pageNo = 1;
		}
		if (pageSize == 0) {
			pageSize = 10;
		}
		List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		list = powerDao.getUserList(userName, deptCode, pageNo, pageSize);
		if (list.size() >= (pageSize * pageNo)) {
			users = list.subList(pageSize * (pageNo - 1), pageSize * pageNo);
		} else if (list.size() < (pageSize * pageNo) && list.size() >= (pageSize * (pageNo - 1))) {
			users = list.subList(pageSize * (pageNo - 1), list.size());
		}
		rs.put("pageNo", pageNo);
		rs.put("result", users);

		return rs;
	}

	//查询部门列表
	@Override
	public Page<Map<String, String>> getDeptList(String deptName, int pageNo, int pageSize) {
		// TODO Auto-generated method stub
		Page<Map<String, String>> page = new Page<Map<String, String>>();

		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		if (pageNo == 1) {
			Map<String, String> alldept = new HashMap<String, String>();
			alldept.put("deptCode", "01");
			alldept.put("deptName", "全部科室");
			result.add(alldept);
		}
		//全部结果
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		list = powerDao.getDeptList(deptName);
		if (list.size() == 0) {
			return page;
		}

		if (list.size() > ((pageNo - 1) * pageSize)) {
			//左闭右开
			if (list.size() < pageNo * pageSize) {
				result = list.subList((pageNo - 1) * pageSize, list.size());
			} else if (list.size() >= pageNo * pageSize) {
				result = list.subList((pageNo - 1) * pageSize, pageNo * pageSize);
			}
			page.setPageNo(pageNo);
			page.setOrderBy("deptCode");
			page.setOrderDir("desc");
			page.setPageSize(pageSize);
			page.setCountTotal(true);
			page.setTotalCount(list.size());
			page.setResult(result);
		} else {
			pageNo = 1;
			//左闭右开
			if (list.size() < pageNo * pageSize) {
				result = list.subList((pageNo - 1) * pageSize, list.size());
			} else if (list.size() >= pageNo * pageSize) {
				result = list.subList((pageNo - 1) * pageSize, pageNo * pageSize);
			}
			page.setPageNo(pageNo);
			page.setOrderBy("deptCode");
			page.setOrderDir("desc");
			page.setPageSize(pageSize);
			page.setCountTotal(true);
			page.setTotalCount(list.size());
			page.setResult(result);
		}
		return page;
	}

	@Override
	public Map<String, String> initDeptPower() {
		// TODO Auto-generated method stub
		//删除权限表
		Map<String, String> rs = new HashMap<String, String>();
		powerDao.deleteDeptPower();
		powerDao.deleteUserPower();
		try {
			List<Map<String, String>> depts = new ArrayList<Map<String, String>>();
			depts = powerDao.getAllDept();
			for (int i = 0; i < depts.size(); i++) {
				insertDeptPower(depts.get(i).get("deptCode"));
				System.out.println("初始化第" + i + "科室的权限");
			}
		} catch (Exception e) {
			rs.put("result", "0");
			e.printStackTrace();
			return rs;
		}
		rs.put("result", "1");
		return rs;
	}

	private void insertDeptPower(String deptCode) {
		// TODO Auto-generated method stub
		DateFormat bf = new SimpleDateFormat("yyyy-MM-dd E a HH:mm:ss");
		String date = bf.format(new Date());
		if ("current".equals(Config.getCIV_CURRENT_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "Current", Config.getCIV_CURRENT_VALUE(), date);
		}

		if ("specialty".equals(Config.getCIV_SPECIALTY_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "Specialty", Config.getCIV_SPECIALTY_VALUE(), date);
		}

		if ("timeaxis".equals(Config.getCIV_TIMEAXIS_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "TimeAxis", Config.getCIV_TIMEAXIS_VALUE(), date);
		}
		if ("specialtytimeaxis".equals(Config.getCIV_SPECIALTYTIMEAXIS_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "SpecialtyTimeAxis", Config.getCIV_SPECIALTYTIMEAXIS_VALUE(),
					date);
		}
		if ("medicalview".equals(Config.getCIV_MEDICAL_VALUE())) {
			powerDao.insertPowerConfigByDept(deptCode, "MedicalView", Config.getCIV_MEDICAL_VALUE(), date);
		}
		powerDao.insertPowerConfigByDept(deptCode, "Visit", Config.getCIV_VISIT_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Category", Config.getCIV_CATEGORY_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Mr", Config.getCIV_EMR_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Exam", Config.getCIV_EXAM_VALUE(), date);
		powerDao.insertPowerConfigByDept(deptCode, "Pathology", Config.getCIV_PATHOLOGY_VALUE(), date);
	}

	/**
	 * 获取脱敏字段配置
	 *
	 * @return
	 */
	public List<Map<String, String>> getInfoHiddenField(String code) {
		List<Map<String, String>> list = powerDao.getSysHideConfig(code);
		return list;
	}

	/**
	 * 更新脱敏配置字段
	 *
	 * @return
	 */
	public boolean updateSysHideConfig(String code, String value, String enabled) {
		return powerDao.updateSysHideConfig(code, value, enabled);
	}

	/**
	 * List<Map>获取脱敏数据
	 *
	 * @return
	 */
	public void getInfoHidden(List<Map<String, String>> info) {
		Map<String, String> config = this.getSysConfigByType("StartUse_HidePatKeyM");
		if ("0".equals(config.get("result"))) {
			return;
		}
		Map<String, String> fields = new HashMap<String, String>();
		//脱敏字段配置
		List<Map<String, String>> list = this.getInfoHiddenField("");
		Map<String, Object> rule = new HashMap<String, Object>();
		for (Map<String, String> map : list) {
			Map<String, String> map1 = new HashMap<String, String>();
			map1.put("inuse", map.get("inuse"));
			map1.put("pattern", map.get("value"));
			String ormFields = map.get("orm_fileds");
			if (StringUtils.isNotBlank(ormFields)) {
				String[] ormFieldss = ormFields.split(",");
				for (String field : ormFieldss) {
					fields.put(field.toUpperCase(), map.get("code"));
				}
			}
			rule.put(map.get("code"), map1);
		}
		for (Map<String, String> map : info) {
			for (String field : fields.keySet()) {
				String fieldValue = fields.get(field);
				Map<String, String> ruleMap = (Map<String, String>) rule.get(fieldValue);
				if (null != ruleMap && "1".equals(ruleMap.get("inuse"))) {
					String pattern = ruleMap.get("pattern");
					String value = map.get(field);
					map.put(field, Utils.encrypt(value, pattern));
				}
			}
		}
	}

	/**
	 * 获取脱敏数据
	 *
	 * @return
	 */
	public String getInfoHidden(String info, String type) {
		Map<String, String> config = this.getSysConfigByType("StartUse_HidePatKeyM");
		if ("0".equals(config.get("result"))) {
			return info;
		}
		String result = info;
		String code = "";
		switch (type) {
		case "name":
			code = "INFO_HIDDEN_NAME";
			break;
		case "no":
			code = "INFO_HIDDEN_NO";
			break;
		case "phone":
			code = "INFO_HIDDEN_PHONE";
			break;
		case "cardNo":
			code = "INFO_HIDDEN_CARD_NO";
			break;
		}
		List<Map<String, String>> list = this.getInfoHiddenField(code);
		Map<String, String> map = new HashMap<String, String>();
		if (null == list || list.size() == 0) {
			return result;
		} else {
			map = list.get(0);
		}
		if ("1".equals(map.get("inuse"))) {
			result = Utils.encrypt(info, map.get("value"));
		}
		return result;
	}


	/**
	 * 获取通用配置
	 *
	 * @param userCode
	 * @return
	 */
	@Override
	public Map<String, Object> getCommonConfig(String userCode) {
		Map<String, Object> res = new HashMap<String, Object>();
		Map<String, String> rs = getCheckAdmin(userCode);
		res.put("CIV_ADMIN", rs);
		//是否启用闭环
		String  value = Config.getConfigValue("StartUse_OrderClose");
		Map<String, String> map =new HashMap<>();//getSysConfigByType("StartUse_OrderClose");
		map.put("result","true".equalsIgnoreCase(value)?"1":"0");
		res.put("StartUse_OrderClose", map);
		//是否隐藏医嘱相关的table操作列
		Map<String, String> result = Config.getCIV_CATEGARY_ORDER_SHOW_CONFIG();
		res.put("CIV_CATEGARY_ORDER_SHOW_CONFIG", result);
		//获取菜单
		Map<String, String> pagePower = getPowerConfigByPage(userCode);
		res.put("pagePower", pagePower);
        //患者末次信息，信息和标签配置
        Map<String, String> configLastVisit = orderService.getPatLastInfoViewConfig();
        res.put("viewConfig", configLastVisit);
        Map<String,Object> logMap = Config.getLogoInfo();
		res.put("logoInfo",logMap);
		//过敏程度过滤条件是否在页面展示
		Map<String, Object> allergyFilter = Config.getAllergyFilter();
		res.put("allergyFilter",allergyFilter);
		Map<String, Object> patientListSwitch =Config.getExchangePatient();
		res.put("patientListConfig",patientListSwitch);
		return res;
	}

	/**
	 * 获取患者列表页配置
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getPatListConfig() {
		Map<String, Object> res = new HashMap<String, Object>();
		//获取查询条件的标签和类型
		Map<String, Object> patListConf = patientListService.getPatListQueryViewConfig();
		res.put("patQueryConf", patListConf);
		//获取患者列表表格的表头
		String tableHead = Config.getCIV_PATIENT_COLUMN();
		res.put("tableHead", tableHead);
		//获取点击查看跳转的目的URL页面
		String defaultPage = Config.getCIV_DEFAULT_PAGE();
		res.put("defaultPage", defaultPage);
		return res;
	}

	/**
	 * 获取当前视图配置
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getCurrentConfig() {
		Map<String, Object> res = new HashMap<String, Object>();
		//模板配置
		String pageModule = Config.getCIV_CURRENT_CONFIG();
		res.put("pageModule", pageModule);
		//全局设置
		List<Map<String, String>> list = getSysConfig();
		res.put("sysConfig",list);
		//医嘱状态
		List<String> orderStatus = Config.getCIV_ORDERSTATUS();
		res.put("orderStatus",orderStatus);
		//检验详情表格表头配置
		res.put("inspectHead",Config.getCIV_LAB_REPORT_DETAIL_HEAD());
		return res;
	}

	/**
	 * 获取就诊试图配置
	 *
	 * @param userCode
	 * @return
	 */
	@Override
	public Map<String, Object> getVisitConfig(String userCode) {
		Map<String, Object> res = new HashMap<String, Object>();
		//检验详情表格表头配置
		res.put("inspectHead",Config.getCIV_LAB_REPORT_DETAIL_HEAD());
		//就诊试图科室筛选
		String deptShow = Config.getVisitDeptSelectConfig();
		res.put("deptShow", deptShow);
		//右侧菜单项
		List<Map<String, Object>> list = getPowerConfigByVisit(userCode);
        res.put("visitShow",list);
		//是否显示就诊次
        res.put("visitIdShow", Config.getCIV_HIDDEN_VISITlIST_VISITID());
        //获取医嘱状态名称列表
        res.put("orderStatus",Config.getCIV_ORDERSTATUS());
        //是否显示右侧菜单数量
        res.put("tabNum",getSysConfigByType("CivVisit_CalcNum"));
        //是否有手术列表项
        res.put("operListConfig",Config.getSHOW_OPER_LIST());
        //手术记录的tabs项
        res.put("operTabConfig", operService.getAnesthesiaConfig());
        // 护理表格头部设置
        res.put("nurseTableHeadConfig",Config.getCIV_NURSE_TABLE_HEAD());
        //费用表格头部设置
		List<String> feeConfigList  =  new ArrayList<String>();
		feeConfigList.add( ConfigCache.getCache("FEE_TABLE_OUT_CHARGE_CONFIG"));
		feeConfigList.add( ConfigCache.getCache("FEE_TABLE_IN_CHARGE_CONFIG"));
        res.put("feeTableHeadcConfig", feeConfigList);
        //检查检验是否显示超时
		String  isOverTimeLab = ConfigCache.getCache("LAB_OVER_TIME");
		String  isOverTimeExam = ConfigCache.getCache("EXAM_OVER_TIME");
		res.put("lab_over_time", "true" == isOverTimeLab ? "1" : "0");
		res.put("exam_over_time", "true" == isOverTimeExam ? "1" : "0");
		//是否使用病例章节模板
		String emrDgHtml = Config.getIS_EMR_DG_HTML();
		res.put("visit_emr_dg", "true" == emrDgHtml ? "1" : "0");
		//就诊试图是否显示病区
		String visitBq = Config.getIS_SHOW_VISIT_CARD_BQ();
		res.put("visit_bq", "true" == visitBq ? "1" : "0");
		//检查检验类一直是否显示超时
		boolean examOverTime = Config.getEXAM_OVER_TIME();
		res.put("exam_over_time", examOverTime);
		boolean labOverTime = Config.getLAB_OVER_TIME();
		res.put("lab_over_time", labOverTime);
		//末次就诊栏是否跟随就诊视图的变化而变化
		String scnnar_show_with_visit = Config.getSCNNAR_SHOW_WITH_VISIT();
		res.put("visit_scnnar_show_config",scnnar_show_with_visit);
		//就诊试图 医嘱显示列配置
		String  orderConfig = Config.getVISIT_ORDER_ALL_SHOW_CONFIG();
		res.put("order_config",orderConfig);
		return res;
	}

    /**
     * 获取分类视图配置
     *
     * @param userCode
     * @return
     */
    @Override
    public Map<String, Object> getCatagoryConfig(String userCode,String patientId,String visitId ) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("inspectHead",Config.getCIV_LAB_REPORT_DETAIL_HEAD());
        String config = Config.getSHOW_OPER_LIST();
        res.put("operListConfig", config);
        //若未配置外部url,获取手术麻醉单配置
        List<Map<String, String>> list = operService.getAnesthesiaConfig();
        res.put("operTabConfig", list);
        //护理单表头
		List<Map<String, String>> rs = Config.getCIV_NURSE_TABLE_HEAD();
		res.put("nurseHeadConfig", rs);
		//费用明细展示表头
		List<String> feeConfigList  =  new ArrayList<String>();
		feeConfigList.add( ConfigCache.getCache("FEE_TABLE_OUT_CHARGE_CONFIG"));
		feeConfigList.add( ConfigCache.getCache("FEE_TABLE_IN_CHARGE_CONFIG"));
		res.put("feeHeadConfig", feeConfigList);
        //若配置外部url,获取配置的外部url
		// 医嘱显示列配置
		String  orderConfig = Config.getVISIT_ORDER_ALL_SHOW_CONFIG();
		res.put("order_config",orderConfig);
        return res;
    }




}
