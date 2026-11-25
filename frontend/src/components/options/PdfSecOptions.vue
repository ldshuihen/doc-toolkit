<script setup>
defineProps({ opt: { type: Object, required: true } })
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="操作模式">
      <el-radio-group v-model="opt.mode" class="seg-group">
        <el-radio-button value="encrypt">加密 PDF</el-radio-button>
        <el-radio-button value="decrypt">解密 PDF</el-radio-button>
      </el-radio-group>
    </el-form-item>

    <el-form-item :label="opt.mode === 'decrypt' ? '打开密码' : '打开密码（可选）'">
      <el-input v-model="opt.userPwd" type="password" show-password clearable
                :placeholder="opt.mode === 'decrypt' ? '请输入当前 PDF 的密码' : '留空则无需密码即可查看'" />
    </el-form-item>

    <template v-if="opt.mode === 'encrypt'">
      <el-form-item label="权限密码（可选）">
        <el-input v-model="opt.ownerPwd" type="password" show-password clearable
                  placeholder="用于限制打印/复制，留空则同打开密码" />
      </el-form-item>

      <el-form-item label="文档权限">
        <div class="perms">
          <el-checkbox v-model="opt.allowPrint">允许打印</el-checkbox>
          <el-checkbox v-model="opt.allowCopy">允许复制文本</el-checkbox>
        </div>
      </el-form-item>
    </template>
  </el-form>
</template>

<style scoped>
.perms { display: flex; gap: 18px; flex-wrap: wrap; }
</style>
