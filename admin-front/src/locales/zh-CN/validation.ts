// 校验原因受控词表（任务书 7.2）：只允许这六个键，禁止自创 reason
export default {
  required: '不能为空',
  len: '长度不符合要求',
  pattern: '格式不正确',
  email: '邮箱格式不正确',
  range: '超出允许范围',
  type: '取值不合法'
}
