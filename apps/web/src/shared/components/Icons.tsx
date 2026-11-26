type IconProps = {
  className?: string
  title?: string
}

type BrandIconProps = IconProps & {
  src: string
  size?: number
}

/** Official Cielo SVGs from assets_cielo (local copies under /brand). */
function BrandIcon({ src, className, title, size = 24 }: BrandIconProps) {
  return (
    <img
      className={className}
      src={src}
      width={size}
      height={size}
      alt={title ?? ''}
      aria-hidden={title ? undefined : true}
      draggable={false}
    />
  )
}

export function IconCheck({ className, title }: IconProps) {
  return (
    <BrandIcon
      className={className}
      src="/brand/icon-check-round.svg"
      title={title}
      size={20}
    />
  )
}

export function IconShield({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-shield.svg" size={22} />
}

export function IconWallet({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-cart.svg" size={24} />
}

export function IconList({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-bar.svg" size={24} />
}

export function IconBank({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-store-id.svg" size={24} />
}

export function IconInfo({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-info-id.svg" size={16} />
}

export function IconPeople({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-duo.svg" size={24} />
}

export function IconRoute({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-route.svg" size={24} />
}

/** White+#00AEEF glyph for cyan tiles (Cielo about icons). */
export function IconMoneyTile({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-money-id.svg" size={28} />
}

export function IconEdit({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="m14.1 5.7 4.2 4.2M5.2 18.8l1.15-4.35L15.95 5.05a1.9 1.9 0 0 1 2.7 0l.55.55a1.9 1.9 0 0 1 0 2.7L9.55 17.7 5.2 18.8Z"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function IconArrowRight({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-arrow.svg" size={16} />
}

export function IconSpark({ className }: IconProps) {
  return <BrandIcon className={className} src="/brand/icon-line.svg" size={24} />
}
