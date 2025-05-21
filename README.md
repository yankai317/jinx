### 说明
controller层是提供给前端的http接口定义。前端代码在jinx-ui下。
注意: 遇到mastergo链接，调用mastergo工具

### 约束
1. 使用post请求时，使用请求对象传递参数。请求对象放置到jinx-api/src/main/java/com/learn/dto包下。禁止使用Map传递参数。
2. 使用get请求时，最多支持两个参数，超过两个参数禁止使用get。
3. 业务实现放置到service层。controller层做入参检查。具体的业务逻辑需要放置到service层。使用符合业务的service对象，不要把全部代码写在一个文件中
4. 参考 jinx-client/src/main/java/com/learn/controller/category/CategoryController.java 的实现
5. 需要时注意使用公共模块提供的service方法。必要时，可以为公共模块接口提供controller接口。不需要了解公共模块接口实现，也不要修改公共模块的代码，。
6. 真实业务项目，所有代码都是线上场景使用的，禁止模拟数据/禁止使用mock数据/禁止跳过业务逻辑/禁止写todo/禁止忽略业务逻辑
7. 登录接口返回token，使用sessionStorage存储，后续在所有requestApi的header里的Authorization字段中携带
8. controller引入 import jakarta.servlet.http.HttpServletRequest; 并使用userTokenUtil 获取userInfoResponse
9. 请阅读 specifications.md 文件。并且遵循其中的逻辑
10. 以下是公共模块说明
# 公共模块

## 1.公共模块-范围
   - java对应类: com.learn.service.CommonRangeInterface
   - 支持业务功能: 可见范围、协同管理、任务指派等功能，都使用该接口操作
   - 通用范围处理表，支持功能，其他功能模块创建部门、角色、人员范围的时候。提供统一的能力。
   - 业务模块包括: 课程、培训、学习地图
### 提供的方法:
     1. 创建范围方法:[batchCreateRange]，输入功能模块类型【比如可见范围】，
        输入业务模块类型[比如培训、课程、学习地图]和id(单个)，
        输入目标范围的类型[部门、角色、人员]和ids(批量)
    2. 查询范围方法: [queryBusinessIdsByTargets]。根据部门、角色、人员id。查询是否在范围内，需要利用到mysql的全文索引。
    3. 查询业务id配置方法[queryRangeConfigByBusinessId]
    4. 根据业务id删除配置方法[deleteRangeByBusinessId]
    5. 更新数据方法[updateRangeByBusinessId]

## 2. 公共模块-用户管理/协同权限校验
   - java对应类: com.learn.service.CommonRangeInterface
   - 提供方法: checkUserHasRight
1. 判断这个用户是否有这个 [功能枚举+业务模块类型+业务模块id] 的权限
   a. 需要先查询用户的角色、部门、人员id
   b. 根据角色、部门、人员id、功能枚举、业务模块类型、业务模块id，查询角色、部门、人员id是否在范围内

## 3. 公共模块-权限
### 权限接口代码请看
后端代码接口见： jinx/jinx-client/src/main/java/com/learn/controller/auth/RbacController.java

## 公共模块-文件模块
   ### 文件上传：
    说明：前端使用，文件上传后获得加密的urlKey
    java对应类: com.learn.controller.file.FileController#uploadFile
    api: /api/file/upload

## 4. 公共模块-部门
### 部门接口代码请看 
后端代码接口见：  jinx-client/src/main/java/com/learn/controller/org/OrgController.java

## 5. 公共模块-用户
### 用户接口代码请看
后端代码接口见： jinx/jinx-client/src/main/java/com/learn/controller/user/UserController.java

## 6. 公共模块-角色
### 角色接口代码请看
后端代码接口见： jinx/jinx-client/src/main/java/com/learn/controller/role/RoleController.java
