/* 常量定义 */

/** 协同编辑类型 */
export const COLLABORATOR_TYPE = {
  /** 不指定 */
  // NONE: {
  //   key: 'NONE',
  //   name: '不指定',
  // },
  /** 全部管理员 */
  ALL: {
    key: 'ALL',
    name: '全部管理员',
  },
  /** 部分人员 */
  PART: {
    key: 'PART',
    name: '部分人员',
  },
};

/** 可见范围类型 */
export const VISIBILITY_TYPE = {
  /** 全员可见 */
  ALL: {
    key: 'ALL',
    name: '全员可见',
  },
  /** 部分范围 */
  PART: {
    key: 'PART',
    name: '部分范围',
  },
};
