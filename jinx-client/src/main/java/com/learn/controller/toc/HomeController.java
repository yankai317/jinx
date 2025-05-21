package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.home.BannerResponseDTO;
import com.learn.dto.toc.home.HomeDataDTO;
import com.learn.infrastructure.repository.entity.Banner;
import com.learn.service.banner.BannerService;
import com.learn.service.toc.HomeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * C端首页控制器
 */
@RestController
@RequestMapping("/api/toc/home")
@Slf4j
public class HomeController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private HomeService homeService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取首页数据
     *
     * @return 首页数据响应
     */
    @GetMapping("/data")
    public ApiResponse<HomeDataDTO> getHomeData() {
        try {
            // 1. 验证token，获取用户ID
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return ApiResponse.error(401, "用户未登录或登录已过期");
            }

            // 2. 调用服务获取首页数据
            HomeDataDTO homeData = homeService.getHomeData(userId);

            // 3. 返回结果
            return ApiResponse.success(homeData);
        } catch (Exception e) {
            log.error("获取首页数据异常", e);
            return ApiResponse.error("获取首页数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取首页轮播图列表
     *
     * @param type 类型
     * @return 轮播图列表响应
     */
    @GetMapping("/banners")
    public ApiResponse<List<BannerResponseDTO>> getBanners(@RequestParam String type) {
        try {
            // 1. 验证token，获取用户ID
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                log.warn("用户未登录或登录已过期");
                return ApiResponse.error(401, "用户未登录或登录已过期");
            }

            // 2. 调用服务获取轮播图列表
            List<Banner> bannerList = bannerService.getBannerList(type);

            // 3. 转换为DTO
            List<BannerResponseDTO> bannerResponseDTOList = convertToBannerResponseDTOList(bannerList);

            // 4. 返回结果
            return ApiResponse.success(bannerResponseDTOList);
        } catch (Exception e) {
            log.error("获取首页轮播图列表异常", e);
            return ApiResponse.error("获取首页轮播图列表失败: " + e.getMessage());
        }
    }

    /**
     * 将Banner实体列表转换为BannerResponseDTO列表
     *
     * @param bannerList Banner实体列表
     * @return BannerResponseDTO列表
     */
    private List<BannerResponseDTO> convertToBannerResponseDTOList(List<Banner> bannerList) {
        List<BannerResponseDTO> bannerResponseDTOList = new ArrayList<>();
        if (bannerList != null && !bannerList.isEmpty()) {
            for (Banner banner : bannerList) {
                BannerResponseDTO bannerResponseDTO = new BannerResponseDTO();
                bannerResponseDTO.setId(banner.getId());
                bannerResponseDTO.setTitle(banner.getTitle());
                bannerResponseDTO.setImageUrl(banner.getImageUrl());
                bannerResponseDTO.setLinkUrl(banner.getLinkUrl());
                bannerResponseDTO.setSort(banner.getSort());
                bannerResponseDTO.setType(banner.getType());
                bannerResponseDTOList.add(bannerResponseDTO);
            }
        }
        return bannerResponseDTOList;
    }
}
