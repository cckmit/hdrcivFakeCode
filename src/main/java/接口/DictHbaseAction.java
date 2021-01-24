package 接口;

import com.goodwill.core.orm.Page;
import com.goodwill.core.utils.json.JsonUtil;
import com.goodwill.hdr.civ.base.action.CIVAction;
import com.goodwill.hdr.civ.enums.DictType;
import com.goodwill.hdr.civ.web.service.DictHbaseService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * 类描述：hbase字典查询类
 * @author peijiping
 * @Date 2018年7月10日
 * @modify
 * 修改记录：
 *
 */
public class DictHbaseAction extends CIVAction {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private DictHbaseService dictHbaseService;

	/**
	 * @Description
	 * 获取检查分类字典
	 * @throws Exception
	 */
	public void getExamClassDict() throws Exception {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.EXAMCLASS, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获取检验明细项字典
	 */
	public void getLabSubDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.LABSUB, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 获取科室字典
	 */
	public void getDeptDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.DEPT, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述:查询护理医嘱字典
	 * @return 返回类型： void
	 * @throws Exception
	 */
	public void getNurseDict() throws Exception {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.NURSE, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述:查询手术医嘱字典
	 * @return 返回类型： void
	 * @throws Exception
	 */
	public void getOperDict() throws Exception {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.OPER, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述: 查询手术ICD9字典
	 * @return 返回类型： void
	 * @throws Exception
	 */
	public void getOperICD9Dict() throws Exception {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.OPERICD9, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述: 查询诊断字典
	 * @return 返回类型： void
	 */
	public void getDiagDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.DIAG, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述: 查询药品医嘱字典
	 * @return 返回类型： void
	 */
	public void getDrugDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.DRUG, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述:查询检查医嘱字典
	 * @return 返回类型： void
	 */
	public void getExamDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.EXAM, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述:查询门诊检查医嘱字典
	 * @return 返回类型： void
	 */
	public void getOutExamDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getOutExamDict(DictType.EXAM_MZ,
				column == null ? "" : column, keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述: 查询检验医嘱字典
	 * @return 返回类型： void
	 */
	public void getLabDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.LAB, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * 获取门诊检验字典编码
	 * @return
	 */
	public void getOutLabDict() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDict(DictType.LAB_MZ, column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 方法描述:查询检验报告明细字典
	 * @return 返回类型： void
	 */
	public void getLabSubDict_temp() {
		String column = getParameter("column");
		String keyword = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//如果select_value值为空 则直接查询

		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		Page<Map<String, String>> result = dictHbaseService.getDictSubLab(column == null ? "" : column,
				keyword == null ? "" : keyword, page);
		renderJson(JsonUtil.getJSONString(result));
	}

	/**
	 * @Description
	 * 根据code获取诊断编码名称
	 */
	public void viewDiagByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.DIAG, codes, page, false)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	/**
	 * @Description
	 * 根据code获取手术编码名称
	 */
	public void viewOperByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.OPER, codes, page, false)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	/**
	 * @Description
	 * 根据code获取手术编码名称
	 */
	public void viewNurseByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.NURSE, codes, page, false)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	/**
	 * @Description
	 * 方法描述:根据编码获取检查医嘱字典
	 * @return 返回类型： void
	 */
	public void viewExamByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.EXAM, codes, page, false)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	/**
	 * @Description
	 * 方法描述:根据编码获取检验字典
	 * @return 返回类型： void
	 */
	public void viewLabByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.LAB, codes, page, false)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	/**
	 * @Description
	 * 方法描述:根据编码获取检验明细字典
	 * @return 返回类型： void
	 */
	public void viewLabSubByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(
					JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.LABSUB, codes, page, false)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	/**
	 * @Description
	 * 方法描述:根据编码获取药品医嘱字典
	 * @return 返回类型： void
	 */
	public void viewDrugByCode() {
		String codes = getParameter("keyword");
		String pageSize = getParameter("pageSize");
		String pageNo = getParameter("pageNo");
		//设置页面和页码
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page.setPageNo(Integer.parseInt(pageNo));
		page.setPageSize(Integer.parseInt(pageSize));
		List<String> codeList = new ArrayList<String>();
		if (StringUtils.isNotBlank(codes)) {
			this.renderJson(JsonUtil.getJSONString(dictHbaseService.getNamebyCode(DictType.DRUG, codes, page, true)));
		} else {
			this.renderJson(JsonUtil.getJSONString(page));
		}
	}

	public void getLabSubType() {
		String search = getParameter("keyWord");
		int pageNo = Integer.valueOf(getParameter("pageNo"));
		int pageSize = Integer.valueOf(getParameter("pageSize"));
		Page<Map<String, String>> page = new Page<Map<String, String>>();
		page = dictHbaseService.getLabSubTypeList(search, pageNo, pageSize);

		renderJson(JsonUtil.getJSONString(page));
	}
}
