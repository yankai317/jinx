
//自动导入所有svg图标
const requireAll = requireContext => requireContext.keys().map(requireContext);
const req = require.context('@/assets/icons', false, /\.svg$/);
requireAll(req);
