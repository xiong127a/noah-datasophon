<template>
  <svg :class="svgClass" aria-hidden="true" v-on="$attrs" style="cursor: pointer;">
    <use :xlink:href="iconName" />
  </svg>
</template>

<script>
export default {
  name: 'SvgIcon',
  props: {
    iconClass: {
      type: String,
      required: true,
    },
    className: {
      type: String,
      default: '',
    },
  },
  computed: {
    iconName() {
      return `#icon-${this.iconClass.toLowerCase()}`;
    },
    svgClass() {
      if (this.className) {
        return `svg-icon ${this.className}`;
      }
      return `svg-icon svg-icon-${this.iconClass.toLowerCase()}`;
    },
  },
};
</script>

<style>
.svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.2em;
  fill: currentColor;
  overflow: hidden;
  font-size: inherit;
  display: inline-flex;
}

/* 针对历史操作图标的特殊样式 */
.svg-icon-setting {
  width: 20px;
  height: 20px;
  font-size: 20px;
}
</style> 